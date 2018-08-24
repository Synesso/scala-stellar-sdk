package stellar.sdk

import java.io.File

import com.github.tomakehurst.wiremock.WireMockServer
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.Amount.lumens
import stellar.sdk.ProxyMode.{NoProxy, Record, Replay}
import stellar.sdk.inet.TxnFailure
import stellar.sdk.op._
import stellar.sdk.resp._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.sys.process._

/**
  * Requires a newly launched stand-alone instance of stellar when running in NoProxy mode.
  * @see src/it/bin/stellar_standalone.sh
  */
class LocalNetworkIntegrationSpec(implicit ee: ExecutionEnv) extends Specification with DomainMatchersIT {
  sequential

  // Set to NoProxy when writing tests; Record when creating stub mappings; Replay for all other times.
  val mode = Replay

  val proxy: Option[WireMockServer] = mode match {
    case NoProxy => None
    case Record =>
      require(new File("src/test/resources/mappings").listFiles().forall(_.delete))
      val s = new WireMockServer(8080)
      Seq("src/it/bin/stellar_standalone.sh", "true").!
      s.start()
      s.startRecording("http://localhost:8000/")
      Some(s)
    case Replay =>
      val s = new WireMockServer(8080)
      s.start()
      None
  }

  private implicit val network = StandaloneNetwork(if (mode == NoProxy) 8000 else 8080)
  val masterAccountKey = network.masterAccount
  var masterAccount = Await.result(network.account(masterAccountKey).map(_.toAccount), 10.seconds)

  val accnA = KeyPair.fromSecretSeed(ByteArrays.sha256("account a".getBytes("UTF-8")))
  val accnB = KeyPair.fromSecretSeed(ByteArrays.sha256("account b".getBytes("UTF-8")))
  val accnC = KeyPair.fromSecretSeed(ByteArrays.sha256("account c".getBytes("UTF-8")))

  val accounts = Set(accnA, accnB, accnC)

  private def transact(ops: Operation*): Unit = {
    val batchSize = 100
    def forReal(batch: Seq[Operation], remaining: Seq[Operation]): Unit = {
      batch match {
        case Nil =>
        case xs =>
          val opAccountIds = xs.flatMap(_.sourceAccount).map(_.accountId).toSet
          val signatories = accounts.filter(a => opAccountIds.contains(a.accountId))
          val signedTransaction = xs.foldLeft(Transaction(masterAccount))(_ add _).sign(masterAccountKey)
          val trp = signatories.foldLeft(signedTransaction)(_ sign _).submit()
          Await.result(trp, 5 minutes)
          masterAccount = masterAccount.withIncSeq
          forReal(remaining.take(batchSize), remaining.drop(batchSize))
      }
    }
    forReal(ops.take(batchSize), ops.drop(batchSize))
  }

  private def setupFixtures: Future[(Account, Account)] = {
    val futureAccountA = network.account(accnA).map(_.toAccount)
    val futureAccountB = network.account(accnB).map(_.toAccount)
    futureAccountA.flatMap(a => futureAccountB.map(b => (a, b))).recoverWith {
      // todo - fixtures should include every kind of operation
      case _ => // account details not found, assume fixture setup is required then try again
        // docker container running inside travis has trouble processing non-trivial transactions within timeout.
        // so, group the operations into smaller transactions.
        transact(
          CreateAccountOperation(accnA, lumens(1000)),
          CreateAccountOperation(accnB, lumens(1000)),
          CreateAccountOperation(accnC, lumens(1000)),
          WriteDataOperation("life_universe_everything", "42", Some(accnB)),
          WriteDataOperation("fenton", "FENTON!", Some(accnC)),
          DeleteDataOperation("fenton", Some(accnC)),
          SetOptionsOperation(setFlags = Some(Set(AuthorizationRequiredFlag, AuthorizationRevocableFlag)), sourceAccount = Some(accnA)),
          ChangeTrustOperation(IssuedAmount(100000000, Asset("Aardvark", accnA)), Some(accnB)),
          ChangeTrustOperation(IssuedAmount(100000000, Asset("Beaver", accnA)), Some(accnB)),
          ChangeTrustOperation(IssuedAmount(100000000, Asset("Chinchilla", accnA)), Some(accnB)),
          ChangeTrustOperation(IssuedAmount(100000000, Asset("Chinchilla", masterAccountKey)), Some(accnA)),
          AllowTrustOperation(accnB, "Aardvark", authorize = true, Some(accnA)),
          AllowTrustOperation(accnB, "Chinchilla", authorize = true, Some(accnA)),
          PaymentOperation(accnB, IssuedAmount(555, Asset("Aardvark", accnA)), Some(accnA))
        )

        // force a transaction boundary between CreateAccount and AccountMerge
        transact(
          AccountMergeOperation(accnB, Some(accnC)),
          CreateOfferOperation(lumens(3), Asset("Aardvark", accnA), Price(3, 100), Some(accnB)),
          CreateOfferOperation(lumens(5), Asset("Chinchilla", accnA), Price(5, 100), Some(accnB)),
          CreatePassiveOfferOperation(IssuedAmount(10, Asset("Beaver", accnA)), NativeAsset, Price(1, 3), Some(accnA)),
          AllowTrustOperation(accnB, "Aardvark", authorize = false, Some(accnA))
        )

        // force a transaction boundary between Create*Offer and Update/DeleteOffer
        transact(
          UpdateOfferOperation(2, lumens(5), Asset("Chinchilla", accnA), Price(1, 5), Some(accnB)),
          DeleteOfferOperation(3, Asset("Beaver", accnA), NativeAsset, Price(1, 3), Some(accnA)),
          InflationOperation(),
          // master pays accnB (chinchilla, master) -> (chinchilla accna) because accnA offers chinchilla
          CreateOfferOperation(IssuedAmount(1, Asset("Chinchilla", accnA)), Asset("Chinchilla", masterAccountKey), Price(1, 1), Some(accnA)),
          PathPaymentOperation(IssuedAmount(1, Asset("Chinchilla", masterAccountKey)), accnB, IssuedAmount(1, Asset("Chinchilla", accnA)), Nil)
        )

        setupFixtures
    }

  }

  val (accountA, accountB) = Await.result(setupFixtures, 5 minutes /* for travis */)

  "account endpoint" should {
    "fetch account response details" >> {
      network.account(accnA) must beLike[AccountResp] {
        case AccountResp(id, _, _, _, _, _, balances, _) =>
          id mustEqual accnA
          balances must containTheSameElementsAs(Seq(
            lumens(1000),
            IssuedAmount(1, Asset.apply("Chinchilla", masterAccountKey))
          ))
      }.awaitFor(30 seconds)
    }

    "fetch nothing for an account that has been merged" >> {
      network.account(accnC) must throwA[TxnFailure].awaitFor(30 seconds)
    }

    "fetch account details from response" >> {
      val kp = accnA
      // #account_details_example
      val account: Future[Account] = network.account(kp).map(_.toAccount)
      val nextSeqNo: Future[Long] = account.map(_.sequenceNumber)
      // #account_details_example
      account.map(_.publicKey) must beEqualTo(kp.asPublicKey).awaitFor(30.seconds)
      nextSeqNo must beGreaterThanOrEqualTo(0L).awaitFor(30.seconds)
    }

    "fetch nothing if no account exists" >> {
      val kp = KeyPair.fromSecretSeed(ByteArrays.sha256("何物".getBytes("UTF-8")))
      network.account(kp) must throwA[TxnFailure].awaitFor(5.seconds)
    }

    "return the data for an account" >> {
      network.accountData(accnB, "life_universe_everything") must beEqualTo("42").awaitFor(5.seconds)
    }

    "fetch nothing if no data exists for the account" >> {
      network.accountData(accnB, "brain_size_of_planet") must throwA[TxnFailure].awaitFor(5.seconds)
    }
  }

  "asset endpoint" should {
    "list all assets" >> {
      val eventualResps = network.assets().map(_.toSeq)
      eventualResps must containTheSameElementsAs(Seq(
        AssetResp(Asset("Aardvark", accnA), 0, 0, authRequired = true, authRevocable = true),
        AssetResp(Asset("Beaver", accnA), 0, 0, authRequired = true, authRevocable = true),
        AssetResp(Asset("Chinchilla", accnA), 1, 1, authRequired = true, authRevocable = true),
        AssetResp(Asset("Chinchilla", masterAccountKey), 1, 1, authRequired = false, authRevocable = false)
      )).awaitFor(10 seconds)
    }

    "filter assets by code" >> {
      val byCode = network.assets(code = Some("Chinchilla"))
      byCode.map(_.size) must beEqualTo(2).awaitFor(10 seconds)
      byCode.map(_.map(_.asset.code).toSet) must beEqualTo(Set("Chinchilla")).awaitFor(10 seconds)
    }

    "filter assets by issuer" >> {
      val byIssuer = network.assets(issuer = Some(accnA)).map(_.take(10).toList)
      byIssuer.map(_.size) must beEqualTo(3).awaitFor(10 seconds)
      byIssuer.map(_.map(_.asset.issuer.accountId).distinct) must beEqualTo(Seq(accnA.accountId)).awaitFor(10 seconds)
    }

    "filter assets by code and issuer" >> {
      network.assets(code = Some("Chinchilla"), issuer = Some(accnA)).map(_.toList)
        .map(_.map(_.asset)) must beLike[Seq[NonNativeAsset]] {
        case Seq(only) => only must beEquivalentTo(IssuedAsset12("Chinchilla", accnA))
      }.awaitFor(10 seconds)
    }
  }

  "effect endpoint" should {
    "parse all effects" >> {
      val effects = network.effects()
      effects.map(_.size) must beEqualTo(29).awaitFor(10 seconds)
    }
  }

  "filter effects by account" >> {
    val byAccount = network.effectsByAccount(accnA).map(_.take(10).toList)
    byAccount.map(_.size) must beEqualTo(9).awaitFor(10 seconds)
    byAccount.map(_.head) must beLike[EffectResp] {
      case EffectAccountCreated(_, account, startingBalance) =>
        account.accountId mustEqual accnA.accountId
        startingBalance mustEqual lumens(1000)
    }.awaitFor(10.seconds)
  }

  "filter effects by ledger" >> {
    val byLedger = network.effectsByLedger(0).map(_.toList)
    byLedger.map(_.head) must beLike[EffectResp] {
      case EffectAccountCreated(_, account, startingBalance) =>
        account.accountId mustEqual accnA.accountId
        startingBalance mustEqual lumens(1000)
    }.awaitFor(10.seconds)
  }

  "filter effects by transaction hash" >> {
    val byTransaction = network.effectsByTransaction("a631c8617c47b735967352755ac305f4230fdfc4385c2d8815934cdc41877cff").map(_.toList)
    byTransaction must beLike[List[EffectResp]] {
      case List(
        EffectAccountDebited(_, accn1, amount1),
        EffectAccountCredited(_, accn2, amount2),
        EffectAccountRemoved(_, accn3),
        EffectTrustLineDeauthorized(_, accn4, IssuedAsset12(code, accn5))
      ) =>
        accn1 must beEquivalentTo(accnC)
        accn2 must beEquivalentTo(accnB)
        accn3 must beEquivalentTo(accnC)
        accn4 must beEquivalentTo(accnB)
        accn5 must beEquivalentTo(accnA)
        amount1 mustEqual lumens(1000)
        amount2 mustEqual lumens(1000)
        code mustEqual "Aardvark"
    }.awaitFor(10.seconds)
  }

  "filter effects by operation" >> {
    (for {
      operationId <- network.operations().map(_.find(_.operation == AccountMergeOperation(accnB, Some(accnC))).get.id)
      byOperation <- network.effectsByOperation(operationId).map(_.toSeq)
    } yield byOperation) must beLike[Seq[EffectResp]] {
      case Seq(
        EffectAccountDebited(_, accn1, amount1),
        EffectAccountCredited(_, accn2, amount2),
        EffectAccountRemoved(_, accn3)) =>
        accn1 must beEquivalentTo(accnC)
        accn2 must beEquivalentTo(accnB)
        accn3 must beEquivalentTo(accnC)
        amount1 mustEqual lumens(1000)
        amount2 mustEqual lumens(1000)
    }.awaitFor(10.seconds)
  }

"ledger endpoint" should {
  val ledgers = network.ledgers().map(_.filter(_.operationCount > 0))
  val firstLedger = ledgers.map(_.head)

  "list the details of a given ledger" >> {
    val ledger = for {
      seq <- firstLedger.map(_.sequence)
      l <- network.ledger(seq)
    } yield l

    firstLedger.zip(ledger) must beLike[(LedgerResp, LedgerResp)] {
      case (l, r) => l mustEqual r
    }.awaitFor(10.seconds)
  }
}

  /*
"offer endpoint" should {
  "list offers by account" >> {
    network.offersByAccount(KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"))
      .map(_.toSeq) must beEqualTo(Seq(
      OfferResp(
        id = 101542,
        seller = KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"),
        selling = Amount.lumens(165),
        buying = IssuedAsset12(
          code = "sausage",
          issuer = KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK")),
        price = Price(303, 100)
      )
    )).awaitFor(10.seconds)
  }
}

"operation endpoint" should {
  "list all operations" >> {
    val oneThirty = network.operations().map(_.take(130))
    oneThirty.map(_.distinct.size) must beEqualTo(130).awaitFor(10.seconds)
  }
  "list operations by account" >> {
    network.operationsByAccount(accnB).map(_.drop(1).head) must beLike[Transacted[Operation]] {
      case op =>
        op.operation mustEqual ChangeTrustOperation(
          IssuedAmount(99, Asset("ScalaSDKSpec", accnA)),
          Some(accnB.asPublicKey)
        )
    }.awaitFor(10.seconds)
  }

  val kinPayment = Transacted(
    id = 70009259709968385L,
    txnHash = "233ce5d17477706e097f72ae1c46241f4586ad1476d191119d46a93e88b9d3fa",
    createdAt = ZonedDateTime.parse("2018-02-16T09:37:30Z"),
    operation = PaymentOperation(
      destinationAccount = KeyPair.fromAccountId("GCT4TTKW2HPCMHM6PJHQ33FIIDCVKIJXLXDHMKQEC7DKHPPGLUKCHKY7"),
      amount = IssuedAmount(28553980000000L,
        IssuedAsset4("KIN", KeyPair.fromAccountId("GBDEVU63Y6NTHJQQZIKVTC23NWLQVP3WJ2RI2OTSJTNYOIGICST6DUXR"))),
      sourceAccount = Some(KeyPair.fromAccountId("GDBWXSZDYO4C3EHYXRLCGU3NP55LUBEQO5K2RWIWWMXWVI57L7VUWSZA"))
    )
  )

  "list operations by ledger" >> {
    PublicNetwork.operationsByLedger(16300301).map(_.last) must beEqualTo(kinPayment).awaitFor(10.seconds)
  }
  "list operations by transaction" >> {
    PublicNetwork.operationsByTransaction("233ce5d17477706e097f72ae1c46241f4586ad1476d191119d46a93e88b9d3fa")
      .map(_.head) must beEqualTo(kinPayment).awaitFor(10.seconds)
  }
  "list the details of a given operation" >> {
    PublicNetwork.operation(70009259709968385L) must beEqualTo(kinPayment).awaitFor(10.seconds)
  }
}

"orderbook endpoint" should {
  "fetch current orders" >> {
    // todo - replace with a static test network assertion
    val mobi = IssuedAsset4("MOBI", KeyPair.fromAccountId("GA6HCMBLTZS5VYYBCATRBRZ3BZJMAFUDKYYF6AH6MVCMGWMRDNSWJPIH"))
    Await.result(PublicNetwork.orderBook(
      selling = NativeAsset,
      buying = mobi
    ), 10.seconds) must beLike { case ob: OrderBook =>
      ob.selling mustEqual NativeAsset
      ob.buying mustEqual mobi
    }
  }
}

"payments endpoint" should {
  "fetch payments in pages" >> {
    network.payments().map(_.take(130).last) must beEqualTo(
      Transacted(
        id = 91946659876865L,
        txnHash = "dd667058cb84fef012a102e5c6be22c532534f0182076d7eabced3e606b22d7d",
        createdAt = ZonedDateTime.parse("2017-03-21T16:06:42Z"),
        operation = PaymentOperation(
          destinationAccount = KeyPair.fromAccountId("GD7E76FQQDNM5GXQX3SMEN5FIZKVHD2KYRYN2UPDQIDKHSXV4QUN7ZT3"),
          amount = IssuedAmount(
            units = 10000L,
            asset = IssuedAsset4(
              "USD",
              KeyPair.fromAccountId("GBK4EP3WICCDJQ3MSYUNRV3PNVQQZAESFGV4DALFENUFAGOY4J7QQNGW")
            )),
          sourceAccount = Some(KeyPair.fromAccountId("GBK4EP3WICCDJQ3MSYUNRV3PNVQQZAESFGV4DALFENUFAGOY4J7QQNGW"))))
    ).awaitFor(10.seconds)
  }

  "filter payments by account" >> {
    PublicNetwork.paymentsByAccount(KeyPair.fromAccountId("GDKUP3J2MXYLSMU556XDSPLGPH5NFITGRT3HSNGNCZP3HTYBZ6AVNB7N"))
      .map(_.drop(9).head) must beEqualTo(
      Transacted(
        id = 68867133416685569L,
        txnHash = "1459b596c081eb87829c9168e9eb044eebc434fd92e4b8bc59a195dbf5c4c123",
        createdAt = ZonedDateTime.parse("2018-02-02T08:55:47Z"),
        operation = PaymentOperation(
          destinationAccount = KeyPair.fromAccountId("GAHK7EEG2WWHVKDNT4CEQFZGKF2LGDSW2IVM4S5DP42RBW3K6BTODB4A"),
          amount = Amount.lumens(6440),
          sourceAccount = Some(KeyPair.fromAccountId("GDKUP3J2MXYLSMU556XDSPLGPH5NFITGRT3HSNGNCZP3HTYBZ6AVNB7N"))))
    ).awaitFor(10.seconds)
  }

  "filter payments by ledger" >> {
    PublicNetwork.paymentsByLedger(16034375).map(_.head) must beEqualTo(
      Transacted(
        id = 68867116236808193L,
        txnHash = "4d0c7f118ca939fe10ba3e1facc601814e3e0135991634fe43f019450fa2b5cd",
        createdAt = ZonedDateTime.parse("2018-02-02T08:55:32Z"),
        operation = PaymentOperation(
          destinationAccount = KeyPair.fromAccountId("GAHK7EEG2WWHVKDNT4CEQFZGKF2LGDSW2IVM4S5DP42RBW3K6BTODB4A"),
          amount = Amount.lumens(19999.99998),
          sourceAccount = Some(KeyPair.fromAccountId("GA5XIGA5C7QTPTWXQHY6MCJRMTRZDOSHR6EFIBNDQTCQHG262N4GGKTM"))))
    ).awaitFor(10.seconds)
  }

  "filter payments by transaction" >> {
    PublicNetwork.paymentsByTransaction("1459b596c081eb87829c9168e9eb044eebc434fd92e4b8bc59a195dbf5c4c123")
      .map(_.head) must beEqualTo(
      Transacted(
        id = 68867133416685569L,
        txnHash = "1459b596c081eb87829c9168e9eb044eebc434fd92e4b8bc59a195dbf5c4c123",
        createdAt = ZonedDateTime.parse("2018-02-02T08:55:47Z"),
        operation = PaymentOperation(
          destinationAccount = KeyPair.fromAccountId("GAHK7EEG2WWHVKDNT4CEQFZGKF2LGDSW2IVM4S5DP42RBW3K6BTODB4A"),
          amount = Amount.lumens(6440),
          sourceAccount = Some(KeyPair.fromAccountId("GDKUP3J2MXYLSMU556XDSPLGPH5NFITGRT3HSNGNCZP3HTYBZ6AVNB7N"))))
    ).awaitFor(10.seconds)
  }
}

"trades endpoint" should {
  "fetch trades in pages" >> {
    PublicNetwork.trades().map(_.take(230).last) must beEqualTo(
      Trade(
        id = "39969884779581441-0",
        ledgerCloseTime = ZonedDateTime.parse("2017-02-25T22:51:46Z"),
        offerId = 2585L,
        baseAccount = KeyPair.fromAccountId("GA6KWMT33N63HIV2NZ3AFWENTYI4ZJSNAXQMTQI3XJK5GORN36SETSNF"),
        baseAmount = IssuedAmount(
          units = 200000000,
          asset = IssuedAsset4("PHP", KeyPair.fromAccountId("GBUQWP3BOUZX34TOND2QV7QQ7K7VJTG6VSE7WMLBTMDJLLAW7YKGU6EP"))
        ),
        counterAccount = KeyPair.fromAccountId("GBZ2KAUWAKA7UKYUHTTXQ3QKXG3HTMMP33BLYJIHUTIXSXMOR6OD2ITZ"),
        counterAmount = IssuedAmount(
          units = 4000000,
          asset = IssuedAsset4("USD", KeyPair.fromAccountId("GDLL2FS5TXV5PCXFYJSLRZFCYEV52QY726R4XYBZKICKQLRARR5Q4SSK"))
        ),
        baseIsSeller = true)
    ).awaitFor(10.seconds)
  }

  "filter trades by orderbook" >> {
    PublicNetwork.tradesByOrderBook(
      base = NativeAsset,
      counter = IssuedAsset4("SLT", KeyPair.fromAccountId("GCKA6K5PCQ6PNF5RQBF7PQDJWRHO6UOGFMRLK3DYHDOI244V47XKQ4GP"))
    ).map(_.take(10).last) must beEqualTo(
      Trade(
        id = "61564881559621633-0",
        ledgerCloseTime = ZonedDateTime.parse("2017-11-02T15:34:05Z"),
        offerId = 187430L,
        baseAccount = KeyPair.fromAccountId("GDLHSCWRFUNEEJL6PR67OZL7QVO2L57MKQOMS6LGKNLGZPX6KCHXREMP"),
        baseAmount = Amount.lumens(0.1457620),
        counterAccount = KeyPair.fromAccountId("GAYDG77BFUUHYXC4IMFGXNBFDS5TMBB545Q6MON3EXYXHDOEJWU2LD2P"),
        counterAmount = IssuedAmount(
          units = 100000L,
          asset = IssuedAsset4("SLT", KeyPair.fromAccountId("GCKA6K5PCQ6PNF5RQBF7PQDJWRHO6UOGFMRLK3DYHDOI244V47XKQ4GP"))
        ),
        baseIsSeller = false)
    ).awaitFor(10.seconds)
  }

  "filter trades by offer id" >> {
    PublicNetwork.tradesByOfferId(283606L).map(_.take(10).last) must beEqualTo(
      Trade(
        id = "3748308153536513-0",
        ledgerCloseTime = ZonedDateTime.parse("2015-11-18T16:59:37Z"),
        offerId = 59L,
        baseAccount = KeyPair.fromAccountId("GAVH5JM5OKXGMQDS7YPRJ4MQCPXJUGH26LYQPQJ4SOMOJ4SXY472ZM7G"),
        baseAmount = Amount.lumens(30),
        counterAccount = KeyPair.fromAccountId("GBB4JST32UWKOLGYYSCEYBHBCOFL2TGBHDVOMZP462ET4ZRD4ULA7S2L"),
        counterAmount = IssuedAmount(
          units = 90000000L,
          asset = IssuedAsset4("JPY", KeyPair.fromAccountId("GBVAOIACNSB7OVUXJYC5UE2D4YK2F7A24T7EE5YOMN4CE6GCHUTOUQXM"))
        ),
        baseIsSeller = true)
    ).awaitFor(10.seconds)
  }
}

"transaction endpoint" should {
  "fetch transactions in pages" >> {
    PublicNetwork.transactions().map(_.take(230).last) must beEquivalentTo(
      TransactionHistoryResp(
        hash = "52190acc9ed1a2cafad9eb050477b7da88a4e1b3ddbd232081bf0a9c6e18e194",
        ledger = 557315,
        createdAt = ZonedDateTime.parse("2015-11-03T23:37:36Z"),
        account = KeyPair.fromAccountId("GAV6F4353XZAYP4OX5AZWYXNMDWSNCAGWKMXQFJRUAV2PEF3RDW2UMIW"),
        sequence = 2373008085745670L,
        feePaid = 100,
        operationCount = 1,
        memo = MemoHash(base64("deRcswHJAMVOb/2YEZS98PMm07UAAAAAAAAAAAAAAAA=")),
        signatures = Seq("YcVM8eFXkkIQk7b3IiWh+eJzYAH9Uhm1U//MtF9Gd52rM+Xi5Q5cSw210gYJXirgXB+9mYdqF1HpxIGEUys/DA=="),
        envelopeXDR = "AAAAACvi833d8gw/jr9Bm2LtYO0miAaymXgVMaArp5C7iO2qAAAAZAAIbj0AAAAGAAAAAAAAAAN15FyzAckAxU5v/ZgRlL3w8ybTtQAAAAAAAAAAAAAAAAAAAAEAAAABAAAAAP1qe44j+i4uIT+arbD4QDQBt8ryEeJd7a0jskQ3nwDeAAAAAAAAAACMeNF28DnnCSCEKAXA9o9jW52f/KL1uSismOAQsmeU7QAAAAAPf0g4AAAAAAAAAAG7iO2qAAAAQGHFTPHhV5JCEJO29yIlofnic2AB/VIZtVP/zLRfRnedqzPl4uUOXEsNtdIGCV4q4FwfvZmHahdR6cSBhFMrPww=",
        resultXDR = "AAAAAAAAAGQAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAA=",
        resultMetaXDR = "AAAAAAAAAAEAAAADAAAAAAAIgQMAAAAAAAAAAIx40XbwOecJIIQoBcD2j2NbnZ/8ovW5KKyY4BCyZ5TtAAAAAA9/SDgACIEDAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAwAIgPYAAAAAAAAAAP1qe44j+i4uIT+arbD4QDQBt8ryEeJd7a0jskQ3nwDeAAKb9aOWO8QACD1BAAAAHgAAAAoAAAAAAAAAAAAAAAABAAAAAAAACgAAAAARC07BokpLTOF+/vVKBwiAlop7hHGJTNeGGlY4MoPykwAAAAEAAAAAK+Lzfd3yDD+Ov0GbYu1g7SaIBrKZeBUxoCunkLuI7aoAAAABAAAAAERmsKL73CyLV/HvjyQCERDXXpWE70Xhyb6MR5qPO3yQAAAAAQAAAABSORGwAdyuanN3sNOHqNSpACyYdkUM3L8VafUu69EvEgAAAAEAAAAAeCzqJNkMM/jLvyuMIfyFHljBlLCtDyj17RMycPuNtRMAAAABAAAAAIEi4R7juq15ymL00DNlAddunyFT4FyUD4muC4t3bobdAAAAAQAAAACaNpLL5YMfjOTdXVEqrAh99LM12sN6He6pHgCRAa1f1QAAAAEAAAAAqB+lfAPV9ak+Zkv4aTNZwGaFFAfui4+yhM3dGhoYJ+sAAAABAAAAAMNJrEvdMg6M+M+n4BDIdzsVSj/ZI9SvAp7mOOsvAD/WAAAAAQAAAADbHA6xiKB1+G79mVqpsHMOleOqKa5mxDpP5KEp/Xdz9wAAAAEAAAAAAAAAAAAAAAEACIEDAAAAAAAAAAD9anuOI/ouLiE/mq2w+EA0AbfK8hHiXe2tI7JEN58A3gACm/WUFvOMAAg9QQAAAB4AAAAKAAAAAAAAAAAAAAAAAQAAAAAAAAoAAAAAEQtOwaJKS0zhfv71SgcIgJaKe4RxiUzXhhpWODKD8pMAAAABAAAAACvi833d8gw/jr9Bm2LtYO0miAaymXgVMaArp5C7iO2qAAAAAQAAAABEZrCi+9wsi1fx748kAhEQ116VhO9F4cm+jEeajzt8kAAAAAEAAAAAUjkRsAHcrmpzd7DTh6jUqQAsmHZFDNy/FWn1LuvRLxIAAAABAAAAAHgs6iTZDDP4y78rjCH8hR5YwZSwrQ8o9e0TMnD7jbUTAAAAAQAAAACBIuEe47qtecpi9NAzZQHXbp8hU+BclA+JrguLd26G3QAAAAEAAAAAmjaSy+WDH4zk3V1RKqwIffSzNdrDeh3uqR4AkQGtX9UAAAABAAAAAKgfpXwD1fWpPmZL+GkzWcBmhRQH7ouPsoTN3RoaGCfrAAAAAQAAAADDSaxL3TIOjPjPp+AQyHc7FUo/2SPUrwKe5jjrLwA/1gAAAAEAAAAA2xwOsYigdfhu/ZlaqbBzDpXjqimuZsQ6T+ShKf13c/cAAAABAAAAAAAAAAA=",
        feeMetaXDR = "AAAAAgAAAAMACH/IAAAAAAAAAAAr4vN93fIMP46/QZti7WDtJogGspl4FTGgK6eQu4jtqgAAAAAdzWMMAAhuPQAAAAUAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAEACIEDAAAAAAAAAAAr4vN93fIMP46/QZti7WDtJogGspl4FTGgK6eQu4jtqgAAAAAdzWKoAAhuPQAAAAYAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAA==")
    ).awaitFor(10.seconds)
  }

  "filter transactions by account" >> {
    val byAccount = network.transactionsByAccount(accnA).map(_.take(10).toList)
    byAccount.map(_.isEmpty) must beFalse.awaitFor(10 seconds)
    byAccount.map(_.head) must beLike[TransactionHistoryResp] {
      case t =>
        t.createdAt.plusMinutes(15).isAfter(ZonedDateTime.now) must beTrue // recent
        t.account mustEqual KeyPair.fromAccountId("GAIH3ULLFQ4DGSECF2AR555KZ4KNDGEKN4AFI4SU2M7B43MGK3QJZNSR")
        t.feePaid mustEqual 100
        t.operationCount mustEqual 1
        t.memo mustEqual NoMemo
    }.awaitFor(10.seconds)
  }

  "filter transactions by ledger" >> {
    val byLedger = PublicNetwork.transactionsByLedger(16237465).map(_.toList)
    byLedger.map(_.size) must beEqualTo(1).awaitFor(10 seconds)
    byLedger.map(_.head) must beLike[TransactionHistoryResp] {
      case t =>
        t.account mustEqual KeyPair.fromAccountId("GA7W7RKIDQMVQ5Y2OCPEVQ5DSLVVN2KUXTLVQ555AZLUMO2Z5JKGKCEH")
        t.feePaid mustEqual 100
        t.operationCount mustEqual 1
        t.memo mustEqual NoMemo
    }.awaitFor(10.seconds)
  }
}

"transaction" should {
  "be accepted when posted to the network" >> {

    val newAccount = KeyPair.random
    val balance = for {
      sequence <- network.account(accnA).map(_.lastSequence + 1)
      txn <- Future {
        // #new_transaction_example
        val account = Account(accnA, sequence)
        Transaction(account)
        // #new_transaction_example
          .add(CreateAccountOperation(newAccount))
          .sign(accnA)
      }
      _ <- txn.submit
      newBalances <- network.account(newAccount).map(_.balances)
    } yield {
      newBalances
    }

    balance must beEqualTo(Seq(Amount.lumens(1))).awaitFor(10.seconds)
  }

  "example of creating and submitting a payment transaction" >> {
    val (payerKeyPair, payeePublicKey) = (accnA, accnB)
    val response =
      // #payment_example
      for {
        sourceAccount <- network.account(payerKeyPair)
        response <- Transaction(sourceAccount)
          .add(PaymentOperation(payeePublicKey, Amount.lumens(5000)))
          .sign(payerKeyPair)
          .submit()
      } yield response
      // #payment_example

    response must haveClass[TransactionPostResp].awaitFor(10.seconds)
  }
*/

  step {
    proxy.foreach(_.stopRecording())
  }

}

/*
object RewritePortTransformer extends ResponseDefinitionTransformer {
  override def transform(req: Request, respDef: ResponseDefinition , files: FileSource, params: Parameters) = {
    ResponseDefinitionBuilder.like(respDef)
      .withBody{
        if (respDef.getBody == null) null else respDef.getBody.replaceAll("localhost:8000", "localhost:8080")
      }.build()
  }

  override def getName: String = "rewrite_port"
}
*/

object ProxyMode extends Enumeration {
  type RecordMode = Value
  val NoProxy, Replay, Record = Value
}

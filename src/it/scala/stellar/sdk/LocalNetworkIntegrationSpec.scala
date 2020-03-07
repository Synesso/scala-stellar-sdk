package stellar.sdk

import java.io.EOFException
import java.time.{Instant, Period}

import com.typesafe.scalalogging.LazyLogging
import okhttp3.HttpUrl
import org.json4s.JsonDSL._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.inet.HorizonEntityNotFound
import stellar.sdk.model.Amount.lumens
import stellar.sdk.model.TimeBounds.Unbounded
import stellar.sdk.model.TradeAggregation.FifteenMinutes
import stellar.sdk.model._
import stellar.sdk.model.op._
import stellar.sdk.model.response._
import stellar.sdk.model.result.TransactionHistory
import stellar.sdk.util.ByteArrays

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.sys.process._

class LocalNetworkIntegrationSpec(implicit ee: ExecutionEnv) extends Specification with DomainMatchersIT with LazyLogging {
  sequential

  Seq("src/it/bin/stellar_standalone.sh", "true").!

  private implicit val network = StandaloneNetwork(HttpUrl.parse(s"http://localhost:8000"))
  val masterAccountKey = network.masterAccount
  var masterAccount = Await.result(network.account(masterAccountKey).map(_.toAccount), 10.seconds)

  val accnA = KeyPair.fromPassphrase("account a")
  val accnB = KeyPair.fromPassphrase("account b")
  val accnC = KeyPair.fromPassphrase("account c")
  val accnD = KeyPair.fromPassphrase("account d")

  logger.debug(s"Account A = ${accnA.accountId}")
  logger.debug(s"Account B = ${accnB.accountId}")
  logger.debug(s"Account C = ${accnC.accountId}")
  logger.debug(s"Account D = ${accnD.accountId}")

  val accounts = Set(accnA, accnB, accnC, accnD)

  val day0Assets = network.assets()
  val day0Effects = network.effects()
  val day0Ledgers = network.ledgers()
  val day0Operations = network.operations()
  val day0Payments = network.payments()
  val day0Trades = network.trades()
  val day0Transactions = network.transactions()

  private def transact(ops: Operation*): Unit = {
    val batchSize = 100

    def forReal(batch: Seq[Operation], remaining: Seq[Operation]): Unit = {
      batch match {
        case Nil =>
        case xs =>
          logger.debug(s"Transacting ${xs.size} operation(s)")
          val opAccountIds = xs.flatMap(_.sourceAccount).map(_.accountId).toSet
          val signatories = accounts.filter(a => opAccountIds.contains(a.accountId))
          val signedTransaction = xs.foldLeft(model.Transaction(masterAccount,
            timeBounds = Unbounded,
            maxFee = NativeAmount(100 * batch.size))
          )(_ add _).sign(masterAccountKey)
          val eventualTransactionPostResponse = signatories.foldLeft(signedTransaction)(_ sign _).submit()
          val transactionPostResponse = Await.result(eventualTransactionPostResponse, 5 minutes)
          transactionPostResponse must beLike[TransactionPostResponse] {
            case a: TransactionApproved =>
              logger.debug(s"Approved. Hash is ${a.hash}")
              a.ledgerEntries // can decode
              a.result // can decode
              a.result.operationResults.foreach(println)
              ok
            case r: TransactionRejected =>
              logger.info(r.detail)
              r.opResultCodes.foreach(s => logger.info(s" - $s"))
              ko
          }
          masterAccount = masterAccount.withIncSeq
          forReal(remaining.take(batchSize), remaining.drop(batchSize))
      }
    }

    forReal(ops.take(batchSize), ops.drop(batchSize))
  }

  // Assets
  private val aardvarkA = Asset("Aardvark", accnA)
  private val beaverA = Asset("Beaver", accnA)
  private val chinchillaA = Asset("Chinchilla", accnA)
  private val chinchillaMaster = Asset("Chinchilla", masterAccountKey)
  private val dachshundB = Asset("Dachshund", accnB)

  // Transaction hashes. These will changed when setup operations change.
  private val txnHash2 = "e13447898b27dbf278d4411022e2e6d0aae78ef70670c7af7834a1f2a6d191d8"
  private val txnHash3 = "2ee32cbbe5f2dceca2934d4f1fa8e41c6661e5270b952fd6a7170ecb314ca0c8"

  private def setupFixtures: Future[(Account, Account)] = {
    val futureAccountA = network.account(accnA).map(_.toAccount)
    val futureAccountB = network.account(accnB).map(_.toAccount)
    futureAccountA.flatMap(a => futureAccountB.map(b => (a, b))).recoverWith {
      case _ => // account details not found, assume fixture setup is required then try again
        // docker container running inside travis has trouble processing non-trivial transactions within timeout.
        // so, group the operations into smaller transactions.
        transact(
          CreateAccountOperation(accnA, lumens(1000)),
          CreateAccountOperation(accnB, lumens(1000)),
          CreateAccountOperation(accnC, lumens(1000)),
          CreateAccountOperation(accnD, lumens(1000)),
          WriteDataOperation("life_universe_everything", "42", Some(accnB)),
          WriteDataOperation("brain the size of a planet", "and they ask me to open a door", Some(accnB)),
          WriteDataOperation("fenton", "FENTON!", Some(accnC)),
          DeleteDataOperation("fenton", Some(accnC)),
          SetOptionsOperation(setFlags = Some(Set(AuthorizationRequiredFlag, AuthorizationRevocableFlag)), sourceAccount = Some(accnA)),
          ChangeTrustOperation(IssuedAmount(100000000, aardvarkA), Some(accnB)),
          ChangeTrustOperation(IssuedAmount(100000000, beaverA), Some(accnB)),
          ChangeTrustOperation(IssuedAmount(100000000, chinchillaA), Some(accnB)),
          ChangeTrustOperation(IssuedAmount(100000000, chinchillaA), Some(accnD)),
          ChangeTrustOperation(IssuedAmount(100000000, chinchillaMaster), Some(accnA)),
          AllowTrustOperation(accnB, "Aardvark", authorize = true, Some(accnA)),
          AllowTrustOperation(accnB, "Chinchilla", authorize = true, Some(accnA)),
          PaymentOperation(accnB, IssuedAmount(555, aardvarkA), Some(accnA))
        )

        // force a transaction boundary between CreateAccount and AccountMerge
        transact(
          AccountMergeOperation(accnB, Some(accnC)),
          CreateSellOfferOperation(lumens(3), aardvarkA, Price(3, 100), Some(accnB)),
          CreateSellOfferOperation(lumens(5), chinchillaA, Price(5, 100), Some(accnB)),
          CreateBuyOfferOperation(dachshundB, Amount(3, aardvarkA), Price(5, 3), Some(accnB)),
          CreatePassiveSellOfferOperation(IssuedAmount(10, beaverA), NativeAsset, Price(1, 3), Some(accnA)),
          AllowTrustOperation(accnB, "Aardvark", authorize = false, Some(accnA))
        )

        // force a transaction boundary between Create*Offer and Update/DeleteOffer
        transact(
          UpdateSellOfferOperation(2, lumens(5), chinchillaA, Price(1, 5), Some(accnB)),
          DeleteSellOfferOperation(4, beaverA, NativeAsset, Price(1, 3), Some(accnA)),
          CreateSellOfferOperation(IssuedAmount(800000000, chinchillaA), NativeAsset, Price(80, 4), Some(accnA)),
          CreateSellOfferOperation(IssuedAmount(10000000, chinchillaA), chinchillaMaster, Price(1, 1), Some(accnA)),
          PathPaymentStrictReceiveOperation(IssuedAmount(1, chinchillaMaster), accnB, IssuedAmount(1, chinchillaA), Nil),
          PathPaymentStrictSendOperation(IssuedAmount(100, chinchillaMaster), accnB, IssuedAmount(1, chinchillaA), Nil),
          BumpSequenceOperation(masterAccount.sequenceNumber + 20),
          SetOptionsOperation(signer = Some(Signer(SHA256Hash(ByteArrays.sha256(dachshundB.encode).toIndexedSeq), 3)), sourceAccount = Some(accnD))
        )

        // example of creating and submitting a payment transaction
        val (payerKeyPair, payeePublicKey) = (accnA, accnB)
        val response =
        // #payment_example
          for {
            sourceAccount <- network.account(payerKeyPair)
            response <- model.Transaction(sourceAccount, timeBounds = Unbounded, maxFee = NativeAmount(100))
              .add(PaymentOperation(payeePublicKey, lumens(5000)))
              .sign(payerKeyPair)
              .submit()
          } yield response
        // #payment_example
        Await.ready(response, 15.seconds)

        // 100 payments
        masterAccount = masterAccount.copy(sequenceNumber = 24) // because of previous bump_sequence op
        transact((1 to 100).map(i => PaymentOperation(accnA, NativeAmount(i))): _*)

        setupFixtures
    }
  }

  val (accountA, accountB) = Await.result(setupFixtures, 5 minutes /* for travis */)

  // This goes first because the stats change as time passes.
  "fee stats" should {
    "be parsed" >> {
      for (feeStats <- network.feeStats()) yield {
        // Changed to a large value because standalone docker needs to wait 5 mins at this time.
        val fiveMinutesPlusFixtureTimeInLedgers = 67L
        feeStats.lastLedger must beCloseTo(fiveMinutesPlusFixtureTimeInLedgers, 6)
      }
    }
  }

  "before fixtures exist" >> {
    "streamed results should be empty" >> {
      day0Assets must beLike[Seq[AssetResponse]] { case xs => xs must beEmpty }.awaitFor(10.seconds)
      day0Effects must beLike[Seq[EffectResponse]] { case xs => xs must beEmpty }.awaitFor(10.seconds)
      day0Operations must beLike[Seq[Transacted[Operation]]] { case xs => xs must beEmpty }.awaitFor(10.seconds)
      day0Payments must beLike[Seq[Transacted[PayOperation]]] { case xs => xs must beEmpty }.awaitFor(10.seconds)
      day0Trades must beLike[Seq[Trade]] { case xs => xs must beEmpty }.awaitFor(10.seconds)
      day0Transactions must beLike[Seq[TransactionHistory]] { case xs => xs must beEmpty }.awaitFor(10.seconds)
    }
  }

  "account endpoint" should {
    "fetch account response details" >> {
      network.account(accnA) must beLike[AccountResponse] {
        case AccountResponse(id, _, _, _, _, _, balances, _, data) =>
          id mustEqual accnA
          balances must containTheSameElementsAs(Seq(
            Balance(lumens(1000.000495), buyingLiabilities = 16000000000L),
            Balance(IssuedAmount(101, Asset.apply("Chinchilla", masterAccountKey)), limit = Some(100000000), 9999899)
          ))
          data must beEmpty
      }.awaitFor(30 seconds)
    }

    "provide access to custom data" >> {
      network.account(accnB) must beLike[AccountResponse] { case r =>
        r.decodedData mustEqual Map(
          "life_universe_everything" -> "42",
          "brain the size of a planet" -> "and they ask me to open a door"
        )
      }.awaitFor(30 seconds)
    }

    "fetch nothing for an account that has been merged" >> {
      network.account(accnC) must throwAn[Exception].like { case HorizonEntityNotFound(uri, body) =>
        body mustEqual ("type" -> "https://stellar.org/horizon-errors/not_found") ~
          ("title" -> "Resource Missing") ~
          ("status" -> 404) ~
          ("detail" -> "The resource at the url requested was not found.  This usually occurs for one of two reasons:  The url requested is not valid, or no data in our database could be found with the parameters provided.")
      }.awaitFor(30 seconds)
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
      val kp = KeyPair.fromSecretSeed(ByteArrays.sha256("何物".getBytes("UTF-8").toIndexedSeq))
      network.account(kp) must throwA[Exception].like { case HorizonEntityNotFound(uri, body) =>
        body mustEqual ("type" -> "https://stellar.org/horizon-errors/not_found") ~
          ("title" -> "Resource Missing") ~
          ("status" -> 404) ~
          ("detail" -> "The resource at the url requested was not found.  This usually occurs for one of two reasons:  The url requested is not valid, or no data in our database could be found with the parameters provided.")
      }.awaitFor(5.seconds)
    }

    "return the data for an account" >> {
      network.accountData(accnB, "life_universe_everything").map(_.toSeq) must
        beEqualTo("42".getBytes("UTF-8").toSeq).awaitFor(5.seconds)
    }

    "fetch nothing if no data exists for the account" >> {
      network.accountData(accnB, "vogon poetry") must throwA[Exception].like { case HorizonEntityNotFound(uri, body) =>
        body mustEqual ("type" -> "https://stellar.org/horizon-errors/not_found") ~
          ("title" -> "Resource Missing") ~
          ("status" -> 404) ~
          ("detail" -> "The resource at the url requested was not found.  This usually occurs for one of two reasons:  The url requested is not valid, or no data in our database could be found with the parameters provided.")
      }.awaitFor(5.seconds)
    }
  }

  "asset endpoint" should {
    "list all assets" >> {
      val eventualResps = network.assets().map(_.toSeq)
      eventualResps must containTheSameElementsAs(Seq(
        // TODO (jem) - Changed because of https://github.com/stellar/go/issues/2369
        // AssetResponse(aardvarkA, 0, 0, authRequired = true, authRevocable = true),
        // AssetResponse(beaverA, 0, 0, authRequired = true, authRevocable = true),
        AssetResponse(chinchillaA, 101, 1, authRequired = true, authRevocable = true),
        AssetResponse(chinchillaMaster, 101, 1, authRequired = false, authRevocable = false),
        // AssetResponse(dachshundB, 0, 0, authRequired = false, authRevocable = false)
      )).awaitFor(10 seconds)
    }

    "filter assets by code" >> {
      val byCode = network.assets(code = Some("Chinchilla"))
      byCode.map(_.size) must beEqualTo(2).awaitFor(10 seconds)
      byCode.map(_.map(_.asset.code).toSet) must beEqualTo(Set("Chinchilla")).awaitFor(10 seconds)
    }

    "filter assets by issuer" >> {
      val byIssuer = network.assets(issuer = Some(accnA)).map(_.take(10).toList)
      // TODO (jem) - Changed because of https://github.com/stellar/go/issues/2369
      // byIssuer.map(_.size) must beEqualTo(3).awaitFor(10 seconds)
      byIssuer.map(_.size) must beEqualTo(1).awaitFor(10 seconds)
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
      effects.map(_.size) must beEqualTo(241).awaitFor(10 seconds)
    }

    "filter effects by account" >> {
      val byAccount = network.effectsByAccount(accnA).map(_.take(10).toList)
      byAccount.map(_.size) must beEqualTo(10).awaitFor(10 seconds)
      byAccount.map(_.head) must beLike[EffectResponse] {
        case EffectAccountCreated(_, account, startingBalance) =>
          account.accountId mustEqual accnA.accountId
          startingBalance mustEqual lumens(1000)
      }.awaitFor(10.seconds)
    }

    "filter effects by ledger" >> {
      val byLedger = network.effectsByLedger(0).map(_.toList)
      byLedger.map(_.head) must beLike[EffectResponse] {
        case EffectAccountCreated(_, account, startingBalance) =>
          account.accountId mustEqual accnA.accountId
          startingBalance mustEqual lumens(1000)
      }.awaitFor(10.seconds)
    }

    "filter effects by transaction hash" >> {
      val byTransaction = network.effectsByTransaction(txnHash2).map(_.toList)
      byTransaction must beLike[List[EffectResponse]] {
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
      } yield byOperation) must beLike[Seq[EffectResponse]] {
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
  }

  "ledger endpoint" should {
    "list the details of a given ledger" >> {
      val ledgers = network.ledgers().map(_.filter(_.operationCount > 0))
      val firstLedger = ledgers.map(_.head)

      val ledger = for {
        seq <- firstLedger.map(_.sequence)
        l <- network.ledger(seq)
      } yield l

      firstLedger.zip(ledger) must beLike[(LedgerResponse, LedgerResponse)] {
        case (l, r) => l mustEqual r
      }.awaitFor(10.seconds)
    }
  }

  "offer endpoint" should {
    "list offers by account" >> {
      network.offersByAccount(accnB).map(_.toSeq) must beLike[Seq[OfferResponse]] {
        case Seq(only) =>
          only.id mustEqual 2
          only.seller must beEquivalentTo(accnB)
          only.selling mustEqual lumens(5)
          only.buying must beEquivalentTo(IssuedAsset12("Chinchilla", accnA))
          only.price mustEqual Price(1, 5)
      }.awaitFor(10.seconds)
    }
  }

  "operation endpoint" should {
    "list all operations" >> {
      network.operations().map(_.size) must beEqualTo(131).awaitFor(10.seconds)
    }

    "list operations by account" >> {
      network.operationsByAccount(accnB).map(_.drop(1).head) must beLike[Transacted[Operation]] {
        case op =>
          op.operation must beEquivalentTo(WriteDataOperation("life_universe_everything", "42", Some(accnB)))
      }.awaitFor(10.seconds)
    }

    "list operations by ledger" >> {
      (for {
        ledgerId <- network.ledgers().map(_.filter(_.operationCount > 0).last.sequence)
        operation <- network.operationsByLedger(ledgerId).map(_.last)
      } yield operation) must beLike[Transacted[Operation]] {
        case op =>
          op.operation must beLike[Operation] {
            case PaymentOperation(dest, amount, source) =>
              dest must beEquivalentTo(accnA)
              amount mustEqual NativeAmount(100)
              source must beSome[PublicKeyOps](masterAccountKey.asPublicKey)
          }
      }.awaitFor(10.seconds)

      "list operations by transaction" >> {
        network.operationsByTransaction(txnHash2)
          .map(_.head) must beLike[Transacted[Operation]] {
          case op =>
            op.operation must beLike[Operation] {
              case AccountMergeOperation(dest, source) =>
                dest must beEquivalentTo(accnB)
                source.map(_.asPublicKey) must beSome(accnC.asPublicKey)
            }
        }.awaitFor(10.seconds)
      }

      "provide operation details" >> {
        (for {
          ops <- network.operationsByAccount(accnB)
          expected = ops.last
          actual <- network.operation(expected.id)
        } yield expected.operation shouldEqual actual.operation).awaitFor(10.seconds)
      }
    }

    "list the details of a given operation" >> {
      network.operationsByTransaction(txnHash3)
        .map(_.drop(1).head) must beLike[Transacted[Operation]] {
        case op =>
          op.operation must beLike[Operation] {
            case DeleteSellOfferOperation(4, selling, NativeAsset, Price(1, 3), source) =>
              selling mustEqual beaverA
              source.map(_.asPublicKey) must beSome(accnA.asPublicKey)
          }
      }.awaitFor(10.seconds)
    }
  }

  "orderbook endpoint" should {
    "fetch current orders" >> {
      network.orderBook(NativeAsset, chinchillaA) must beLike[OrderBook] {
        case OrderBook(NativeAsset, buying, bids, asks) =>
          buying must beEquivalentTo(chinchillaA)
          bids mustEqual Seq(Order(Price(4, 80), 800000000))
          asks mustEqual Seq(Order(Price(1, 5), 50000000))
      }.awaitFor(10.seconds)
    }
  }

  "payments endpoint" should {
    "filter payments by account" >> {
      network.paymentsByAccount(accnA) must beLike[Seq[Transacted[PayOperation]]] {
        case a +: b +: oneHundredPayments =>
          a.operation must beEquivalentTo(CreateAccountOperation(accnA, lumens(1000), Some(masterAccountKey)))
          b.operation must beEquivalentTo(PaymentOperation(accnB, IssuedAmount(555, aardvarkA), Some(accnA)))
          oneHundredPayments must haveSize(100)
      }.awaitFor(10.seconds)
    }

    "filter payments by ledger" >> {
      (for {
        ledgerId <- network.ledgers().map(_.filter(_.operationCount > 0).filter(_.operationCount != 100).last.sequence)
        operation <- network.paymentsByLedger(ledgerId)
      } yield operation) must beLike[Seq[Transacted[PayOperation]]] {
        case Seq(op1, op2) =>
          op1.operation must beEquivalentTo(PathPaymentStrictReceiveOperation(
            IssuedAmount(1, chinchillaMaster), accnB, IssuedAmount(1, chinchillaA), Nil
          ))
          op2.operation must beEquivalentTo(PathPaymentStrictSendOperation(
            IssuedAmount(100, chinchillaMaster), accnB, IssuedAmount(1, chinchillaA), Nil
          ))
      }.awaitFor(10.seconds)
    }

    "filter payments by transaction" >> {
      network.paymentsByTransaction(txnHash3) must
        beLike[Seq[Transacted[PayOperation]]] {
          case Seq(op1, op2) =>
            op1.operation must beEquivalentTo(PathPaymentStrictReceiveOperation(
              IssuedAmount(1, chinchillaMaster), accnB, IssuedAmount(1, chinchillaA), Nil
            ))
            op2.operation must beEquivalentTo(PathPaymentStrictSendOperation(
              IssuedAmount(100, chinchillaMaster), accnB, IssuedAmount(1, chinchillaA), Nil
            ))
        }.awaitFor(10.seconds)
    }
  }

  "trades endpoint" should {
    "filter trades by orderbook" >> {
      network.tradesByOrderBook(
        base = chinchillaA,
        counter = chinchillaMaster
      ) must beLike[Seq[Trade]] {
        case Seq(trade1, trade2) =>
          trade1.baseAccount must beEquivalentTo(accnA)
          trade1.baseAmount must beEquivalentTo(IssuedAmount(1, chinchillaA))
          trade1.counterAccount must beEquivalentTo(masterAccountKey.asPublicKey)
          trade1.counterAmount must beEquivalentTo(IssuedAmount(1, chinchillaMaster))
          trade1.baseIsSeller must beTrue

          trade2.baseAccount must beEquivalentTo(accnA)
          trade2.baseAmount must beEquivalentTo(IssuedAmount(100, chinchillaA))
          trade2.counterAccount must beEquivalentTo(masterAccountKey.asPublicKey)
          trade2.counterAmount must beEquivalentTo(IssuedAmount(100, chinchillaMaster))
          trade2.baseIsSeller must beTrue
      }.awaitFor(10.seconds)
    }
  }

  "transaction endpoint" should {
    "filter transactions by account" >> {
      val byAccount = network.transactionsByAccount(accnA).map(_.take(10).toList)
      byAccount.map(_.isEmpty) must beFalse.awaitFor(10 seconds)
      byAccount.map(_.head) must beLike[TransactionHistory] {
        case t =>
          t.account must beEquivalentTo(masterAccountKey)
          t.feeCharged mustEqual NativeAmount(1700)
          t.operationCount mustEqual 17
          t.memo mustEqual NoMemo
          t.ledgerEntries must not(throwAn[EOFException])
          t.feeLedgerEntries must not(throwAn[EOFException])
      }.awaitFor(10.seconds)
    }

    "filter transactions by ledger" >> {
      (for {
        ledgerId <- network.ledgers(Now, Desc).map(_.filter(_.operationCount > 0).head.sequence)
        operation <- network.transactionsByLedger(ledgerId)
      } yield operation) must beLike[Seq[TransactionHistory]] {
        case Seq(t) =>
          t.account must beEquivalentTo(masterAccountKey)
          t.feeCharged mustEqual NativeAmount(10000)
          t.operationCount mustEqual 100
          t.memo mustEqual NoMemo
      }.awaitFor(10.seconds)
    }
  }

  "trade aggregations endpoint" should {
    "show aggregations for a given pair" >> {
      val start = Instant.now().minus(Period.ofDays(1))
      val end = Instant.now().plus(Period.ofDays(1))
      network.tradeAggregations(start, end, FifteenMinutes, 0, chinchillaA, chinchillaMaster) must beLike[Seq[TradeAggregation]] {
        case Seq(ta) =>
          ta.instant.isBefore(Instant.now()) must beTrue
          ta.tradeCount mustEqual 2
          ta.average mustEqual 1
          ta.baseVolume mustEqual 1.01e-5
          ta.counterVolume mustEqual 1.01e-5
          ta.open mustEqual Price(1, 1)
          ta.high mustEqual Price(1, 1)
          ta.low mustEqual Price(1, 1)
          ta.close mustEqual Price(1, 1)
      }.awaitFor(10.seconds)
    }
  }

  "fee stats endpoint" should {
    "return the stats for the last ledger" >> {
      network.feeStats() must beLike[FeeStatsResponse]({ case fsr =>
        fsr.chargedFees.min mustEqual NativeAmount(100)
      }).awaitFor(10 seconds)
    }
  }

  "payment path endpoint" should {
    "return valid payment paths" >> {
      network.paths(masterAccountKey, accnD, Amount(1, chinchillaA)) must
        beEqualTo(Seq(
          PaymentPath(lumens(20), Amount(10000000, chinchillaA), List())
        )).awaitFor(1 minute)
    }

    "return nothing when there's no path" >> {
      network.paths(masterAccountKey, accnD, Amount(1, dachshundB)) must
        beEmpty[Seq[PaymentPath]].awaitFor(1 minute)
    }
  }

  "page deserialisation" should {
    "return an empty page when the underlying resource does not exist" >> {
      network.horizon.getStream("/does_not_exist", TradeDeserializer, Now, Asc) must beEmpty[LazyList[Trade]]
        .awaitFor(10.seconds)
    }
  }

  "transacting with a hash signer" should {
    "work when it has been added" >> {
      for {
        account <- network.account(accnD)
        txn = Transaction(
          source = account,
          operations = List(CreateAccountOperation(KeyPair.random, startingBalance = lumens(100))),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(100))
          .sign(dachshundB.encode)
        txnResult <- txn.submit()
      } yield txnResult must beLike[TransactionPostResponse] { case r =>
        r.isSuccess must beTrue
      }
    }
  }
}


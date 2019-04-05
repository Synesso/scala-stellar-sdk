package stellar.sdk

import java.net.URI

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.json4s.JsonDSL._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.inet.HorizonEntityNotFound
import stellar.sdk.model.Amount.lumens
import stellar.sdk.model._
import stellar.sdk.model.op._
import stellar.sdk.model.response._
import stellar.sdk.model.result.TransactionHistory
import stellar.sdk.util.ByteArrays

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.{Failure, Success}

class LocalNetworkIntegrationSpec(implicit ee: ExecutionEnv) extends Specification with DomainMatchersIT {
  sequential

  Seq("src/it/bin/stellar_standalone.sh", "true").!

  private implicit val network = StandaloneNetwork(URI.create(s"http://localhost:8000"))
  val masterAccountKey = network.masterAccount
  var masterAccount = Await.result(network.account(masterAccountKey).map(_.toAccount), 10.seconds)

  val accnA = KeyPair.fromPassphrase("account a")
  val accnB = KeyPair.fromPassphrase("account b")
  val accnC = KeyPair.fromPassphrase("account c")

  val accounts = Set(accnA, accnB, accnC)

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
          val opAccountIds = xs.flatMap(_.sourceAccount).map(_.accountId).toSet
          val signatories = accounts.filter(a => opAccountIds.contains(a.accountId))
          val signedTransaction = xs.foldLeft(model.Transaction(masterAccount))(_ add _).sign(masterAccountKey)
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
          CreateOfferOperation(IssuedAmount(80, Asset("Chinchilla", accnA)), NativeAsset, Price(80, 4), Some(accnA)),
          CreateOfferOperation(IssuedAmount(1, Asset("Chinchilla", accnA)), Asset("Chinchilla", masterAccountKey), Price(1, 1), Some(accnA)),
          PathPaymentOperation(IssuedAmount(1, Asset("Chinchilla", masterAccountKey)), accnB, IssuedAmount(1, Asset("Chinchilla", accnA)), Nil),
          BumpSequenceOperation(masterAccount.sequenceNumber + 20)
        )

        // example of creating and submitting a payment transaction
        val (payerKeyPair, payeePublicKey) = (accnA, accnB)
        val response =
        // #payment_example
          for {
            sourceAccount <- network.account(payerKeyPair)
            response <- model.Transaction(sourceAccount)
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

  "before fixtures exist" >> {
    "streamed results should be empty" >> {
      day0Assets must beLike[Seq[AssetResponse]] { case xs => xs must beEmpty } .awaitFor(10.seconds)
      day0Effects must beLike[Seq[EffectResponse]] { case xs => xs must beEmpty } .awaitFor(10.seconds)
      day0Operations must beLike[Seq[Transacted[Operation]]] { case xs => xs must beEmpty } .awaitFor(10.seconds)
      day0Payments must beLike[Seq[Transacted[PayOperation]]] { case xs => xs must beEmpty } .awaitFor(10.seconds)
      day0Trades must beLike[Seq[Trade]] { case xs => xs must beEmpty } .awaitFor(10.seconds)
      day0Transactions must beLike[Seq[TransactionHistory]] { case xs => xs must beEmpty } .awaitFor(10.seconds)
    }
  }

  "account endpoint" should {
    "fetch account response details" >> {
      network.account(accnA) must beLike[AccountResponse] {
        case AccountResponse(id, _, _, _, _, _, balances, _) =>
          id mustEqual accnA
          balances must containTheSameElementsAs(Seq(
            Balance(lumens(1000.000495), buyingLiabilities = 1600),
            Balance(IssuedAmount(1, Asset.apply("Chinchilla", masterAccountKey)), limit = Some(100000000))
          ))
      }.awaitFor(30 seconds)
    }

    "fetch nothing for an account that has been merged" >> {
      network.account(accnC) must throwA[Exception].like { case HorizonEntityNotFound(uri, body) =>
        body mustEqual ("type" -> "https://stellar.org/horizon-errors/not_found") ~
          ("title" -> "Resource Missing") ~
          ("status" -> 404) ~
          ("detail" -> "The resource at the url requested was not found.  This is usually occurs for one of two reasons:  The url requested is not valid, or no data in our database could be found with the parameters provided.")
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
      val kp = KeyPair.fromSecretSeed(ByteArrays.sha256("何物".getBytes("UTF-8")))
      network.account(kp) must throwA[Exception].like { case HorizonEntityNotFound(uri, body) =>
        body mustEqual ("type" -> "https://stellar.org/horizon-errors/not_found") ~
          ("title" -> "Resource Missing") ~
          ("status" -> 404) ~
          ("detail" -> "The resource at the url requested was not found.  This is usually occurs for one of two reasons:  The url requested is not valid, or no data in our database could be found with the parameters provided.")
      }.awaitFor(5.seconds)
    }

    "return the data for an account" >> {
      network.accountData(accnB, "life_universe_everything") must beEqualTo("42").awaitFor(5.seconds)
    }

    "fetch nothing if no data exists for the account" >> {
      network.accountData(accnB, "brain_size_of_planet") must throwA[Exception].like { case HorizonEntityNotFound(uri, body) =>
        body mustEqual ("type" -> "https://stellar.org/horizon-errors/not_found") ~
          ("title" -> "Resource Missing") ~
          ("status" -> 404) ~
          ("detail" -> "The resource at the url requested was not found.  This is usually occurs for one of two reasons:  The url requested is not valid, or no data in our database could be found with the parameters provided.")
      }.awaitFor(5.seconds)
    }
  }

  "asset endpoint" should {
    "list all assets" >> {
      val eventualResps = network.assets().map(_.toSeq)
      eventualResps must containTheSameElementsAs(Seq(
        AssetResponse(Asset("Aardvark", accnA), 0, 0, authRequired = true, authRevocable = true),
        AssetResponse(Asset("Beaver", accnA), 0, 0, authRequired = true, authRevocable = true),
        AssetResponse(Asset("Chinchilla", accnA), 1, 1, authRequired = true, authRevocable = true),
        AssetResponse(Asset("Chinchilla", masterAccountKey), 1, 1, authRequired = false, authRevocable = false)
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
      effects.map(_.size) must beEqualTo(230).awaitFor(10 seconds)
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
      val byTransaction = network.effectsByTransaction("a631c8617c47b735967352755ac305f4230fdfc4385c2d8815934cdc41877cff").map(_.toList)
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
      network.operations().map(_.size) must beEqualTo(126).awaitFor(10.seconds)
    }

    "list operations by account" >> {
      network.operationsByAccount(accnB).map(_.drop(1).head) must beLike[Transacted[Operation]] {
        case op =>
          op.operation mustEqual WriteDataOperation("life_universe_everything", "42", Some(accnB))
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
        network.operationsByTransaction("a631c8617c47b735967352755ac305f4230fdfc4385c2d8815934cdc41877cff")
          .map(_.head) must beLike[Transacted[Operation]] {
          case op =>
            op.operation must beLike[Operation] {
              case AccountMergeOperation(dest, source) =>
                dest must beEquivalentTo(accnB)
                source.map(_.asPublicKey) must beSome(accnC.asPublicKey)
            }
        }.awaitFor(10.seconds)
      }
    }

    "list the details of a given operation" >> {
      network.operationsByTransaction("c5e29c7d19c8af4fa932e6bd3214397a6f20041bc0234dacaac66bf155c02ae9")
        .map(_.drop(2).head) must beLike[Transacted[Operation]] {
        case op =>
          op.operation must beLike[Operation] {
            case InflationOperation(source) =>
              source.map(_.asPublicKey) must beSome(masterAccountKey.asPublicKey)
          }
      }.awaitFor(10.seconds)
    }
  }

  "orderbook endpoint" should {
    "fetch current orders" >> {
      network.orderBook(NativeAsset, Asset("Chinchilla", accnA)) must beLike[OrderBook] {
        case OrderBook(NativeAsset, buying, bids, asks) =>
          buying must beEquivalentTo(Asset("Chinchilla", accnA))
          bids mustEqual Seq(Order(Price(4, 80), 80))
          asks mustEqual Seq(Order(Price(1,5),50000000))
      }.awaitFor(10.seconds)
    }
  }

  "payments endpoint" should {
    "filter payments by account" >> {
      network.paymentsByAccount(accnA) must beLike[Seq[Transacted[PayOperation]]] {
        case a +: b +: oneHundredPayments =>
          a.operation must beEquivalentTo(CreateAccountOperation(accnA, lumens(1000), Some(masterAccountKey)))
          b.operation must beEquivalentTo(PaymentOperation(accnB, IssuedAmount(555, Asset("Aardvark", accnA)), Some(accnA)))
          oneHundredPayments must haveSize(100)
      }.awaitFor(10.seconds)
    }

    "filter payments by ledger" >> {
      (for {
        ledgerId <- network.ledgers().map(_.filter(_.operationCount > 0).filter(_.operationCount != 100).last.sequence)
        operation <- network.paymentsByLedger(ledgerId)
      } yield operation) must beLike[Seq[Transacted[PayOperation]]] {
        case Seq(op) =>
          op.operation must beEquivalentTo(PathPaymentOperation(
            IssuedAmount(1, Asset("Chinchilla", masterAccountKey)), accnB, IssuedAmount(1, Asset("Chinchilla", accnA)), Nil
          ))
      }.awaitFor(10.seconds)
    }

    "filter payments by transaction" >> {
      network.paymentsByTransaction("c5e29c7d19c8af4fa932e6bd3214397a6f20041bc0234dacaac66bf155c02ae9") must
        beLike[Seq[Transacted[PayOperation]]] {
          case Seq(op) =>
            op.operation must beEquivalentTo(PathPaymentOperation(
              IssuedAmount(1, Asset("Chinchilla", masterAccountKey)), accnB, IssuedAmount(1, Asset("Chinchilla", accnA)), Nil
            ))
        }.awaitFor(10.seconds)
    }
  }

  "trades endpoint" should {
    "filter trades by orderbook" >> {
      network.tradesByOrderBook(
        base = Asset("Chinchilla", accnA),
        counter = Asset("Chinchilla", masterAccountKey.asPublicKey)
      ) must beLike[Seq[Trade]] {
        case Seq(trade) =>
          trade.baseAccount must beEquivalentTo(accnA)
          trade.baseAmount must beEquivalentTo(IssuedAmount(1, Asset("Chinchilla", accnA)))
          trade.counterAccount must beEquivalentTo(masterAccountKey.asPublicKey)
          trade.counterAmount must beEquivalentTo(IssuedAmount(1, Asset("Chinchilla", masterAccountKey.asPublicKey)))
          trade.baseIsSeller must beTrue
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
          t.feePaid mustEqual NativeAmount(1400)
          t.operationCount mustEqual 14
          t.memo mustEqual NoMemo
      }.awaitFor(10.seconds)
    }

    "filter transactions by ledger" >> {
      (for {
        ledgerId <- network.ledgers(Now, Desc).map(_.filter(_.operationCount > 0).head.sequence)
        operation <- network.transactionsByLedger(ledgerId)
      } yield operation) must beLike[Seq[TransactionHistory]] {
        case Seq(t) =>
          t.account must beEquivalentTo(masterAccountKey)
          t.feePaid mustEqual NativeAmount(10000)
          t.operationCount mustEqual 100
          t.memo mustEqual NoMemo
      }.awaitFor(10.seconds)
    }
  }

  "fee stats endpoint" should {
    "return the stats for the last ledger" >> {
      network.feeStats() must beLike[FeeStatsResponse]({ case fsr =>
          fsr.minAcceptedFee mustEqual NativeAmount(100)
      }).awaitFor(10 seconds)
    }
  }

  implicit val system = ActorSystem("local-network-integration-spec")
  implicit val materializer = ActorMaterializer()

  "transaction source" should {
    "provide all future transactions" >> {
      val source = network.transactionSource()
      val results: Future[Seq[TransactionHistory]] = source.take(1).runWith(Sink.seq[TransactionHistory])
      results.isCompleted must beFalse
      transact(InflationOperation())
      results.map(_.size) must beEqualTo(1).awaitFor(1 minute)
    }
  }
}


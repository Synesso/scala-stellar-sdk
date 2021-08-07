package stellar.sdk

import com.typesafe.scalalogging.LazyLogging
import okhttp3.HttpUrl
import okio.ByteString
import org.json4s.JsonDSL._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.stellar.xdr.TrustLineFlags
import stellar.sdk.inet.HorizonEntityNotFound
import stellar.sdk.model.Amount.lumens
import stellar.sdk.model.ClaimPredicate.{AbsolutelyBefore, Or, Unconditional}
import stellar.sdk.model.TimeBounds.Unbounded
import stellar.sdk.model.TradeAggregation.FifteenMinutes
import stellar.sdk.model.op._
import stellar.sdk.model.response.AccountResponse.DATA_KEY_MEMO_REQUIRED
import stellar.sdk.model.response._
import stellar.sdk.model.result.TransactionHistory
import stellar.sdk.model.{ClaimableBalance, _}
import stellar.sdk.util.ByteArrays

import java.io.EOFException
import java.time.{Instant, Period}
import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
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
  val accnE = KeyPair.fromPassphrase("account e")
  val accnF = KeyPair.fromPassphrase("account f")

  logger.debug(s"Account A = ${accnA.accountId}")
  logger.debug(s"Account B = ${accnB.accountId}")
  logger.debug(s"Account C = ${accnC.accountId}")
  logger.debug(s"Account D = ${accnD.accountId}")
  logger.debug(s"Account E = ${accnE.accountId}")
  logger.debug(s"Account F = ${accnF.accountId}")

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

    @tailrec
    def forReal(batch: Seq[Operation], remaining: Seq[Operation]): Unit = {
      batch match {
        case Nil =>
        case xs =>
          logger.debug(s"Transacting ${xs.size} operation(s)")
          val opAccountIds = xs.flatMap(_.sourceAccount).map(_.publicKey.accountId).toSet
          val signatories = accounts.filter(a => opAccountIds.contains(a.accountId))
          val signedTransaction = xs.foldLeft(model.Transaction(masterAccount,
            timeBounds = Unbounded,
            maxFee = NativeAmount(100 * batch.size))
          )(_ add _).sign(masterAccountKey)
          val fullySignedTransaction = signatories.foldLeft(signedTransaction)(_ sign _)
          val eventualTransactionPostResponse = fullySignedTransaction.submit()
          val transactionPostResponse = Await.result(eventualTransactionPostResponse, 5.minutes)
          transactionPostResponse must beLike[TransactionPostResponse] {
            case a: TransactionApproved =>
              logger.debug(s"Approved. Hash is ${a.hash}")
              a.ledgerEntries // can decode
              a.result // can decode
              ok
            case r: TransactionRejected =>
              logger.info(r.detail)
              logger.info(s"Result: ${r.resultXDR}")
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
  private val clawbackAsset = Asset("XYZ", accnF)

  // Transaction hashes. These will changed when setup operations change.
  private val txnHash2 = "e13447898b27dbf278d4411022e2e6d0aae78ef70670c7af7834a1f2a6d191d8"
  private val txnHash3 = "2ee32cbbe5f2dceca2934d4f1fa8e41c6661e5270b952fd6a7170ecb314ca0c8"

  private def setupFixtures: Future[(Account, Account)] = {
    val futureAccountA = network.account(accnA).map(_.toAccount)
    val futureAccountB = network.account(accnB).map(_.toAccount)
    futureAccountA.flatMap(a => futureAccountB.map(b => (a, b))).recoverWith {
      case _ => // account details not found, assume fixture setup is required then try again
        transact(
          CreateAccountOperation(accnA.toAccountId, lumens(1000)),
          CreateAccountOperation(accnB.toAccountId, lumens(1000)),
          CreateAccountOperation(accnC.toAccountId, lumens(1000)),
          CreateAccountOperation(accnD.toAccountId, lumens(1000)),
          CreateAccountOperation(accnF.toAccountId, lumens(1000)),
          WriteDataOperation("life_universe_everything", "42", Some(accnB.toAccountId)),
          WriteDataOperation("brain the size of a planet", "and they ask me to open a door", Some(accnB.toAccountId)),
          WriteDataOperation("fenton", "FENTON!", Some(accnC.toAccountId)),
          DeleteDataOperation("fenton", Some(accnC.toAccountId)),
          SetOptionsOperation(setFlags = Some(Set(AuthorizationRequiredFlag, AuthorizationRevocableFlag)), sourceAccount = Some(accnA.toAccountId)),
          ChangeTrustOperation(IssuedAmount(100000000, aardvarkA), Some(accnB.toAccountId)),
          ChangeTrustOperation(IssuedAmount(100000000, beaverA), Some(accnB.toAccountId)),
          ChangeTrustOperation(IssuedAmount(100000000, chinchillaA), Some(accnB.toAccountId)),
          ChangeTrustOperation(IssuedAmount(100000000, chinchillaA), Some(accnD.toAccountId)),
          ChangeTrustOperation(IssuedAmount(100000000, chinchillaMaster), Some(accnA.toAccountId)),
          AllowTrustOperation(accnB, "Aardvark", trustLineFlags = Set(TrustLineAuthorized), Some(accnA.toAccountId)),
          AllowTrustOperation(accnB, "Chinchilla", trustLineFlags = Set(TrustLineAuthorized), Some(accnA.toAccountId)),
          PaymentOperation(accnB.toAccountId, IssuedAmount(555, aardvarkA), Some(accnA.toAccountId))
        )

        // force a transaction boundary between CreateAccount and AccountMerge
        transact(
          AccountMergeOperation(AccountId(accnB.publicKey), Some(accnC.toAccountId)),
          CreateSellOfferOperation(lumens(3), aardvarkA, Price(3, 100), Some(accnB.toAccountId)),
          CreateSellOfferOperation(lumens(5), chinchillaA, Price(5, 100), Some(accnB.toAccountId)),
          CreateBuyOfferOperation(dachshundB, Amount(3, aardvarkA), Price(5, 3), Some(accnB.toAccountId)),
          CreatePassiveSellOfferOperation(IssuedAmount(10, beaverA), NativeAsset, Price(1, 3), Some(accnA.toAccountId)),
          AllowTrustOperation(accnB, "Aardvark", trustLineFlags = Set(), Some(accnA.toAccountId))
        )

        // force a transaction boundary between Create*Offer and Update/DeleteOffer
        transact(
          UpdateSellOfferOperation(2, lumens(5), chinchillaA, Price(1, 5), Some(accnB.toAccountId)),
          DeleteSellOfferOperation(4, beaverA, NativeAsset, Price(1, 3), Some(accnA.toAccountId)),
          CreateSellOfferOperation(IssuedAmount(800000000, chinchillaA), NativeAsset, Price(80, 4), Some(accnA.toAccountId)),
          CreateSellOfferOperation(IssuedAmount(10000000, chinchillaA), chinchillaMaster, Price(1, 1), Some(accnA.toAccountId)),
          PathPaymentStrictReceiveOperation(IssuedAmount(1, chinchillaMaster), accnB.toAccountId, IssuedAmount(1, chinchillaA), Nil),
          PathPaymentStrictSendOperation(IssuedAmount(100, chinchillaMaster), accnB.toAccountId, IssuedAmount(1, chinchillaA), Nil),
          BumpSequenceOperation(masterAccount.sequenceNumber + 20),
          SetOptionsOperation(signer = Some(Signer(SHA256Hash(dachshundB.encode.sha256().toByteArray), 3)), sourceAccount = Some(accnD.toAccountId))
        )

        // example of creating and submitting a payment transaction
        val (payerKeyPair, payeePublicKey) = (accnA, accnB)
        val response =
        // #payment_example
          for {
            sourceAccount <- network.account(payerKeyPair)
            response <- model.Transaction(sourceAccount, timeBounds = Unbounded, maxFee = NativeAmount(100))
              .add(PaymentOperation(payeePublicKey.toAccountId, lumens(5000)))
              .sign(payerKeyPair)
              .submit()
          } yield response
        // #payment_example
        Await.ready(response, 15.seconds)

        // 100 payments
        masterAccount = masterAccount.copy(sequenceNumber = 24) // because of previous bump_sequence op
        transact((1 to 100).map(i => PaymentOperation(accnA.toAccountId, NativeAmount(i))): _*)

        setupFixtures
    }
  }

  val (accountA, accountB) = Await.result(setupFixtures, 5.minutes /* for slow ci servers */)

  // This goes first because the stats change as time passes.
  "fee stats" should {
    "be parsed" >> {
      for (feeStats <- network.feeStats()) yield {
        val fixtureTimeInLedgers = 7L
        feeStats.lastLedger must beCloseTo(fixtureTimeInLedgers, 2)
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
        case AccountResponse(id, _, _, _, _, _, balances, _, _, _, _, data) =>
          id mustEqual accnA
          balances must containTheSameElementsAs(Seq(
            Balance(lumens(1000.000495), buyingLiabilities = 16000000000L),
            Balance(IssuedAmount(101, Asset.apply("Chinchilla", masterAccountKey)),
              limit = Some(100000000),
              buyingLiabilities = 9999899,
              authorized = true,
              authorizedToMaintainLiabilities = true)
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
      account.map(_.id.hash) must beEqualTo(kp.publicKey.toSeq).awaitFor(30.seconds)
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
        AssetResponse(aardvarkA, AssetBalances(0, 0, 555), 0, authRequired = true, authRevocable = true),
        AssetResponse(beaverA, AssetBalances(0, 0, 0), 0, authRequired = true, authRevocable = true),
        AssetResponse(chinchillaA, AssetBalances(101, 0, 0), 1, authRequired = true, authRevocable = true),
        AssetResponse(chinchillaMaster, AssetBalances(101, 0, 0), 1, authRequired = false, authRevocable = false),
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
      effects.map(_.size) must beEqualTo(246).awaitFor(10 seconds)
    }

    "filter effects by account" >> {
      val byAccount = network.effectsByAccount(accnA).map(_.take(10).toList)
      byAccount.map(_.size) must beEqualTo(10).awaitFor(10 seconds)
      byAccount.map(_.head) must beLike[EffectResponse] {
        case EffectAccountCreated(_, _, account, startingBalance) =>
          account mustEqual accnA.toAccountId
          startingBalance mustEqual lumens(1000)
      }.awaitFor(10.seconds)
    }

    "filter effects by ledger" >> {
      val byLedger = network.effectsByLedger(0).map(_.toList)
      byLedger.map(_.head) must beLike[EffectResponse] {
        case EffectAccountCreated(_, _, account, startingBalance) =>
          account mustEqual accnA.toAccountId
          startingBalance mustEqual lumens(1000)
      }.awaitFor(10.seconds)
    }

    "filter effects by transaction hash" >> {
      val byTransaction = network.effectsByTransaction(txnHash2).map(_.toList)
      byTransaction must beLike[List[EffectResponse]] {
        case List(
        EffectAccountDebited(_, _, accn1, amount1),
        EffectAccountCredited(_, _, accn2, amount2),
        EffectAccountRemoved(_, _, accn3),
        EffectTrustLineDeauthorized(_, _, accn4, IssuedAsset12(code, accn5)),
        EffectTrustLineFlagsUpdated(_, _, accn6, asset5, false, false, false)
        ) =>
          accn1.publicKey must beEquivalentTo(accnC)
          accn2.publicKey must beEquivalentTo(accnB)
          accn3.publicKey must beEquivalentTo(accnC)
          accn4 must beEquivalentTo(accnB)
          accn5 must beEquivalentTo(accnA)
          accn6 must beEquivalentTo(accnB)
          asset5 mustEqual aardvarkA
          amount1 mustEqual lumens(1000)
          amount2 mustEqual lumens(1000)
          code mustEqual "Aardvark"
      }.awaitFor(10.seconds)
    }

    "filter effects by operation" >> {
      (for {
        operationId <- network.operations().map(_.find(_.operation == AccountMergeOperation(AccountId(accnB.publicKey), Some(accnC.toAccountId))).get.id)
        byOperation <- network.effectsByOperation(operationId).map(_.toSeq)
      } yield byOperation) must beLike[Seq[EffectResponse]] {
        case Seq(
        EffectAccountDebited(_, _, accn1, amount1),
        EffectAccountCredited(_, _, accn2, amount2),
        EffectAccountRemoved(_, _, accn3)) =>
          accn1.publicKey must beEquivalentTo(accnC)
          accn2.publicKey must beEquivalentTo(accnB)
          accn3.publicKey must beEquivalentTo(accnC)
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
    "list all offers" >> {
      network.offers().map(_.toSeq) must beLike[Seq[OfferResponse]] {
        case Seq(first, second, third) =>
          first.id mustEqual 2
          first.seller must beEquivalentTo(accnB)
          first.selling mustEqual lumens(5)
          first.buying must beEquivalentTo(Asset("Chinchilla", accnA))
          first.price mustEqual Price(1, 5)
          second.id mustEqual 5
          second.seller must beEquivalentTo(accnA)
          second.selling must beEquivalentTo(Amount(800000000, Asset("Chinchilla", accnA)))
          second.buying mustEqual NativeAsset
          second.price mustEqual Price(80, 4)
          third.id mustEqual 6
          third.seller must beEquivalentTo(accnA)
          third.selling must beEquivalentTo(Amount(9999899, Asset("Chinchilla", accnA)))
          third.buying must beEquivalentTo(Asset("Chinchilla", masterAccountKey))
          third.price mustEqual Price(1, 1)
      }.awaitFor(10.seconds)
    }

    "list offers by selling asset" >> {
      network.offers(selling = Some(Asset("Chinchilla", accnA))).map(_.toSeq) must beLike[Seq[OfferResponse]] {
        case Seq(first, second) =>
          first.id mustEqual 5
          first.seller must beEquivalentTo(accnA)
          first.selling must beEquivalentTo(Amount(800000000, Asset("Chinchilla", accnA)))
          first.buying mustEqual NativeAsset
          first.price mustEqual Price(80, 4)
          second.id mustEqual 6
          second.seller must beEquivalentTo(accnA)
          second.selling must beEquivalentTo(Amount(9999899, Asset("Chinchilla", accnA)))
          second.buying must beEquivalentTo(Asset("Chinchilla", masterAccountKey))
          second.price mustEqual Price(1, 1)
      }.awaitFor(10.seconds)
    }

    "list offers by buying asset" >> {
      network.offers(buying = Some(Asset("Chinchilla", accnA))).map(_.toSeq) must beLike[Seq[OfferResponse]] {
        case Seq(only) =>
          only.id mustEqual 2
          only.seller must beEquivalentTo(accnB)
          only.selling mustEqual lumens(5)
          only.buying must beEquivalentTo(Asset("Chinchilla", accnA))
          only.price mustEqual Price(1, 5)
      }.awaitFor(10.seconds)
    }

    "list offers by account" >> {
      network.offers(account = Some(accnA)).map(_.toSeq) must beLike[Seq[OfferResponse]] {
        case Seq(first, second) =>
          logger.debug("Sponsor1=" + first.sponsor)
          logger.debug("Sponsor2=" + second.sponsor)
          first.seller must beEquivalentTo(accnA)
          first.selling must beEquivalentTo(Amount(800000000, Asset("Chinchilla", accnA)))
          first.buying mustEqual NativeAsset
          first.price mustEqual Price(80, 4)
          second.id mustEqual 6
          second.seller must beEquivalentTo(accnA)
          second.selling must beEquivalentTo(Amount(9999899, Asset("Chinchilla", accnA)))
          second.buying must beEquivalentTo(Asset("Chinchilla", masterAccountKey))
          second.price mustEqual Price(1, 1)
      }.awaitFor(10.seconds)
    }
  }

  "account endpoint" should {
    "list offers by account" >> {
      network.offersByAccount(accnB).map(_.toSeq) must beLike[Seq[OfferResponse]] {
        case Seq(only) =>
          only.id mustEqual 2
          only.seller must beEquivalentTo(accnB)
          only.selling mustEqual lumens(5)
          only.buying must beEquivalentTo(Asset("Chinchilla", accnA))
          only.price mustEqual Price(1, 5)
      }.awaitFor(10.seconds)
    }
  }

  "operation endpoint" should {
    "list all operations" >> {
      network.operations().map(_.size) must beEqualTo(132).awaitFor(10.seconds)
    }

    "list operations by account" >> {
      network.operationsByAccount(accnB).map(_.drop(1).head) must beLike[Transacted[Operation]] {
        case op =>
          op.operation must beEquivalentTo(WriteDataOperation("life_universe_everything", "42", Some(accnB.toAccountId)))
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
              dest must beEquivalentTo(accnA.toAccountId)
              amount mustEqual NativeAmount(100)
              source must beSome[AccountId](masterAccountKey.toAccountId)
          }
      }.awaitFor(10.seconds)

      "list operations by transaction" >> {
        network.operationsByTransaction(txnHash2)
          .map(_.head) must beLike[Transacted[Operation]] {
          case op =>
            op.operation must beLike[Operation] {
              case AccountMergeOperation(dest, source) =>
                new ByteString(dest.hash.toArray) mustEqual new ByteString(accnB.publicKey)
                source.map(_.publicKey) must beSome(accnC.asPublicKey)
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
              source.map(_.publicKey) must beSome(accnA.asPublicKey)
          }
      }.awaitFor(10.seconds)
    }
  }

  "orderbook endpoint" should {
    "fetch current orders" >> {
      network.orderBook(NativeAsset, chinchillaA) must beLike[OrderBook] {
        case OrderBook(NativeAsset, buying, bids, asks) =>
          buying must beEquivalentTo(chinchillaA)
          bids mustEqual Seq(Order(Price(1, 20), 800000000))
          asks mustEqual Seq(Order(Price(1, 5), 50000000))
      }.awaitFor(10.seconds)
    }
  }

  "payments endpoint" should {
    "filter payments by account" >> {
      network.paymentsByAccount(accnA) must beLike[Seq[Transacted[PayOperation]]] {
        case a +: b +: oneHundredPayments =>
          a.operation must beEquivalentTo(CreateAccountOperation(accnA.toAccountId, lumens(1000), Some(masterAccountKey.toAccountId)))
          b.operation must beEquivalentTo(PaymentOperation(accnB.toAccountId, IssuedAmount(555, aardvarkA), Some(accnA.toAccountId)))
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
            IssuedAmount(1, chinchillaMaster), accnB.toAccountId, IssuedAmount(1, chinchillaA), Nil
          ))
          op2.operation must beEquivalentTo(PathPaymentStrictSendOperation(
            IssuedAmount(100, chinchillaMaster), accnB.toAccountId, IssuedAmount(1, chinchillaA), Nil
          ))
      }.awaitFor(10.seconds)
    }

    "filter payments by transaction" >> {
      network.paymentsByTransaction(txnHash3) must
        beLike[Seq[Transacted[PayOperation]]] {
          case Seq(op1, op2) =>
            op1.operation must beEquivalentTo(PathPaymentStrictReceiveOperation(
              IssuedAmount(1, chinchillaMaster), accnB.toAccountId, IssuedAmount(1, chinchillaA), Nil
            ))
            op2.operation must beEquivalentTo(PathPaymentStrictSendOperation(
              IssuedAmount(100, chinchillaMaster), accnB.toAccountId, IssuedAmount(1, chinchillaA), Nil
            ))
        }.awaitFor(10.seconds)
    }
  }

  "trades endpoint" should {
    "list all trades in ascending order" >> {
      network.trades(order = Asc).map(_.toList.map(it => it.baseAmount -> it.counterAmount)) must beEqualTo(
        List(
          IssuedAmount(1, chinchillaA) -> IssuedAmount(1, chinchillaMaster),
          IssuedAmount(100, chinchillaA) -> IssuedAmount(100, chinchillaMaster),
        )
      ).awaitFor(10.seconds)
    }

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
          t.account must beEquivalentTo(masterAccountKey.toAccountId)
          t.feeCharged mustEqual NativeAmount(1800)
          t.operationCount mustEqual 18
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
          t.account must beEquivalentTo(masterAccountKey.toAccountId)
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
          operations = List(CreateAccountOperation(KeyPair.random.toAccountId, startingBalance = lumens(100))),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(100))
          .sign(dachshundB.encode.toByteArray)
        txnResult <- txn.submit()
      } yield txnResult must beLike[TransactionPostResponse] { case r =>
        r.isSuccess must beTrue
      }
    }
  }

  "claimable balances" should {
    val instant = Instant.parse("2025-01-20T09:00:00Z")
    val predicate = Or(
      AbsolutelyBefore(instant),
      Unconditional
    )
    val id = ClaimableBalanceHashId(
      ByteString.decodeHex("a20bec491b3901338a39b1430c4ca177176641698a267c1c92df5aaf8b9688ee"))
    val createOperation = CreateClaimableBalanceOperation(
      amount = Amount(5000, dachshundB),
      claimants = List(
        AccountIdClaimant(accnA.asPublicKey, predicate),
        AccountIdClaimant(accnC.asPublicKey, Unconditional)
      ),
      sourceAccount = Some(accnB.toAccountId)
    )
    val claimOperation = ClaimClaimableBalanceOperation(id, Some(accnA.toAccountId))

    "be creatable" >> {
      for {
        account <- network.account(accnB)
        txn = Transaction(
          source = account,
          operations = List(
            createOperation,
            ChangeTrustOperation(IssuedAmount(100000000, dachshundB), Some(accnA.toAccountId)),
          ),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(200)
        ).sign(accnA, accnB)
        txnResult <- txn.submit()
      } yield txnResult must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }
    }

    def includeTheOneWeJustPosted =
      beLike[Seq[ClaimableBalance]] { case seq =>
        seq must haveSize(1)
        val only = seq.head
        only.id mustEqual id
        only.amount mustEqual Amount(5000, dachshundB)
        only.sponsor mustEqual accnB
        only.claimants must containTheSameElementsAs(List(
          AccountIdClaimant(accnA.asPublicKey, predicate),
          AccountIdClaimant(accnC.asPublicKey, Unconditional)
        ))
      }

    "be listable by account" >> {
      network.claimsByClaimant(accnA) must includeTheOneWeJustPosted.awaitFor(10.seconds)
    }

    "be listable by asset" >> {
      network.claimsByAsset(dachshundB) must includeTheOneWeJustPosted.awaitFor(10.seconds)
    }

    "be listable by sponsor" >> {
      network.claimsBySponsor(accnB) must includeTheOneWeJustPosted.awaitFor(10.seconds)
    }

    "be findable by id" >> {
      Await.result(network.claim(id).map(LazyList(_)), 10.seconds) must includeTheOneWeJustPosted
    }

    "be claimable" >> {
      for {
        account <- network.account(accnA)
        _ <- network.claimsByClaimant(accnA).map(_.head.id)
        txn = Transaction(
          source = account,
          operations = List(claimOperation),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(100)
        ).sign(accnA)
        txnResult <- txn.submit()
      } yield txnResult must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }
    }

    "be used to filter operations" >> {
      network.operationsByClaim(id).map(_.toList.map(_.operation)) must
        beEqualTo(List(createOperation, claimOperation)).awaitFor(10.seconds)
    }

    "be used to filter transactions" >> {
      network.transactionsByClaim(id).map(_.toList.size) must beEqualTo(2).awaitFor(10.seconds)
    }
  }

  "creating accounts with no starting balance" should {
    "not be blocked by the SDK" >> {
      for {
        account <- network.account(accnA)
        txn = Transaction(
          source = account,
          operations = List(CreateAccountOperation(accnE.toAccountId, Amount.lumens(0))),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(100)
        ).sign(accnA)
        txnResult <- txn.submit()
      } yield txnResult must beLike[TransactionPostResponse] { case r: TransactionRejected =>
        r.opResultCodes mustEqual List("op_low_reserve")
      }
    }
  }

  "a transaction with payments that need memos" should {

    "refuse to submit if any payments are present but there is no memo" >> {
      Await.ready(for {
        account <- network.account(accnD)
        txn = Transaction(
          source = account,
          operations = List(WriteDataOperation(DATA_KEY_MEMO_REQUIRED, "1")),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(100)
        ).sign(accnD)
        txnResult <- txn.submit()
      } yield txnResult must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }, 10.seconds)

      val attempt = for {
        account <- network.account(accnA)
        txn = Transaction(
          source = account,
          operations = List(PaymentOperation(accnD.toAccountId, Amount.lumens(100))),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(100)
        ).sign(accnA)
        txnResult <- txn.submit()
      } yield txnResult
      attempt must throwAn[InvalidTransactionException].awaitFor(10.seconds)
    }

    "except when an override is in place" >> {
      for {
        account <- network.account(accnA)
        txn = Transaction(
          source = account,
          operations = List(PaymentOperation(accnD.toAccountId, Amount.lumens(100))),
          overrideMemoRequirement = true,
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(100)
        ).sign(accnA)
        txnResult <- txn.submit()
      } yield txnResult must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }
    }

    "submit if any payments are present and there is a memo" >> {
      val attempt = for {
        account <- network.account(accnA)
        txn = Transaction(
          source = account,
          operations = List(PaymentOperation(accnD.toAccountId, Amount.lumens(100))),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(100),
          memo = MemoText("Bananas")
        ).sign(accnA)
        txnResult <- txn.submit()
      } yield txnResult
      attempt must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }.awaitFor(10.seconds)
    }
  }

  "an account that wishes to sponsor" should {
    val sponsoredAccount = KeyPair.random

    "be able to begin a sponsorship" >> {
      val attempt = for {
        account <- network.account(accnA)
        txn = Transaction(
          source = account,
          operations = List(
            BeginSponsoringFutureReservesOperation(sponsoredAccount.toAccountId),
            CreateAccountOperation(sponsoredAccount.toAccountId, startingBalance = Amount.lumens(0)),
            EndSponsoringFutureReservesOperation(sourceAccount = Some(sponsoredAccount.toAccountId))
          ),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(300)
        ).sign(accnA, sponsoredAccount)
        txnResult <- txn.submit()
      } yield txnResult
      attempt must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }.awaitFor(60.seconds)

      network.account(accnA) must beLike[AccountResponse] { case a: AccountResponse =>
        a.reservesSponsoring mustEqual 2
        a.reservesSponsored mustEqual 0
        a.sponsor must beNone
      }.awaitFor(5.seconds)

      network.account(sponsoredAccount) must beLike[AccountResponse] { case a: AccountResponse =>
        a.reservesSponsoring mustEqual 0
        a.reservesSponsored mustEqual 2
        a.sponsor must beSome(accnA.asPublicKey)
      }.awaitFor(5.seconds)
    }
  }

  "Clawbacks" should {
    "be used to revoke funds from an account" >> {
      val setClawbackEnabled = for {
        account <- network.account(masterAccountKey)
        txn = Transaction(
          source = account,
          operations = List(
            SetOptionsOperation(
              setFlags = Some(Set(AuthorizationRevocableFlag, AuthorizationClawbackEnabledFlag)),
              sourceAccount = Some(accnF.toAccountId)
            ),
            ChangeTrustOperation(IssuedAmount(99_999L, clawbackAsset), Some(accnA.toAccountId)),
            PaymentOperation(accnA.toAccountId, IssuedAmount(50_000L, clawbackAsset), Some(accnF.toAccountId))
          ),
        timeBounds = TimeBounds.Unbounded,
        maxFee = NativeAmount(1000)
        ).sign(masterAccountKey, accnF, accnA)
        txnResult <- txn.submit()
      } yield txnResult
      setClawbackEnabled must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }.awaitFor(60.seconds)
      network.account(accnA).map(_.balances) must beLike[List[Balance]] { balances =>
        balances must contain(Balance(IssuedAmount(50_000L, clawbackAsset), Some(99_999L), authorized = true, authorizedToMaintainLiabilities = true))
      }.awaitFor(3.seconds)

      val clawbackTheFunds = for {
        account <- network.account(accnF)
        txn = Transaction(
          source = account,
          operations = List(
            ClawBackOperation(accnA.toAccountId, IssuedAmount(40_000L, clawbackAsset), Some(accnF.toAccountId))
          ),
        timeBounds = TimeBounds.Unbounded,
        maxFee = NativeAmount(1000)
        ).sign(accnF)
        txnResult <- txn.submit()
      } yield txnResult
      clawbackTheFunds must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }.awaitFor(60.seconds)

      network.account(accnA).map(_.balances) must beLike[List[Balance]] { balances =>
        balances must contain(Balance(IssuedAmount(10_000L, clawbackAsset), Some(99_999L), authorized = true, authorizedToMaintainLiabilities = true))
      }.awaitFor(3.seconds)
    }

    "be able to claw back claimable balances" >> {
      Await.result(for {
        account <- network.account(accnF)
        txn = Transaction(
          source = account,
          operations = List(
            CreateClaimableBalanceOperation(
              amount = Amount(1000, clawbackAsset),
              claimants = List(AccountIdClaimant(accnA, Unconditional))
            )
          ),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(200)
        ).sign(accnF)
        txnResult <- txn.submit()
      } yield txnResult must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }, 60.seconds)

      val clawbackTheFunds = for {
        balanceId <- network.claimsByClaimant(accnA).map(_.head.id)
        account <- network.account(accnF)
        txn = Transaction(
          source = account,
          operations = List(ClawBackClaimableBalanceOperation(balanceId)),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(1000)
        ).sign(accnF)
        txnResult <- txn.submit()
      } yield txnResult
      clawbackTheFunds must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }.awaitFor(60.seconds)

      network.claimsByAsset(dachshundB) must beEmpty[Seq[ClaimableBalance]].awaitFor(10.seconds)
    }
  }

  "Trustline flags" should {
    "be settable" >> {
      (for {
        account <- network.account(accnF)
        txn = Transaction(
          source = account,
          operations = List(SetTrustLineFlagsOperation(
            asset = clawbackAsset,
            trustor = accnA,
            setFlags = Set(TrustLineFlags.AUTHORIZED_FLAG),
            clearFlags = Set.empty[TrustLineFlags]
          )),
          timeBounds = TimeBounds.Unbounded,
          maxFee = NativeAmount(1000)
        ).sign(accnF)
        txnResult <- txn.submit()
      } yield txnResult) must beLike[TransactionPostResponse] { case r: TransactionApproved =>
        r.isSuccess must beTrue
      }.awaitFor(60.seconds)

    }
  }
}


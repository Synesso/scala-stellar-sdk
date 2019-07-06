package stellar.sdk

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.apache.commons.codec.binary.Hex
import org.json4s.CustomSerializer
import stellar.sdk.inet.HorizonAccess
import stellar.sdk.model._
import stellar.sdk.model.op._
import stellar.sdk.model.response._
import stellar.sdk.model.result.TransactionHistory

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class DocExamples() {

  //noinspection ScalaUnusedSymbol
  // $COVERAGE-OFF$

  // #sources_implicit_setup
  implicit val system = ActorSystem("stellar-sources")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  // #sources_implicit_setup

  def `query documentation`() = {
    val TestNetwork = new DoNothingNetwork
    // #keypair_from_accountid
    val accountId = "GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"
    val publicKey = KeyPair.fromAccountId(accountId)
    // #keypair_from_accountid

    def `be present for accounts`() = {
      // #account_query_examples
      val accountId = "GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"
      val publicKey = KeyPair.fromAccountId(accountId)

      // account details
      val accountDetails: Future[AccountResponse] = TestNetwork.account(publicKey)

      // account datum value
      val accountData: Future[String] = TestNetwork.accountData(publicKey, "data_key")
      // #account_query_examples
    }

    def `be present for assets`() = {
      // #asset_query_examples
      // stream of all assets from all issuers
      val allAssets: Future[Stream[AssetResponse]] = TestNetwork.assets()

      // stream of the last 20 assets created
      val last20Assets =
        TestNetwork.assets(cursor = Now, order = Desc).map(_.take(20))

      // stream of assets with the code HUG
      val hugAssets: Future[Stream[AssetResponse]] = TestNetwork.assets(code = Some("HUG"))

      // stream of assets from the specified issuer
      val issuerAssets: Future[Stream[AssetResponse]] =
        TestNetwork.assets(issuer = Some(publicKey))

      // Stream (of max length 1) of HUG assets from the issuer
      val issuersHugAsset: Future[Stream[AssetResponse]] =
        TestNetwork.assets(code = Some("HUG"), issuer = Some(publicKey))
      // #asset_query_examples
    }

    def `be present for effects`() = {
      // #effect_query_examples
      // stream of all effects
      val allEffects: Future[Stream[EffectResponse]] = TestNetwork.effects()

      // stream of the last 20 effects
      val last20Effects =
        TestNetwork.effects(cursor = Now, order = Desc).map(_.take(20))

      // stream of effects related to a specific account
      val effectsForAccount = TestNetwork.effectsByAccount(publicKey)

      // stream of effects related to a specific transaction hash
      val effectsForTxn: Future[Stream[EffectResponse]] =
        TestNetwork.effectsByTransaction("f00cafe...")

      // stream of effects related to a specific operation id
      val effectsForOperationId: Future[Stream[EffectResponse]] =
        TestNetwork.effectsByOperation(123L)

      // stream of effects for a specific ledger
      val effectsForLedger = TestNetwork.effectsByLedger(1234)
      // #effect_query_examples

      // #effect_source_examples
      // a source of all new effects
      val effectsSource: Source[EffectResponse, NotUsed] = TestNetwork.effectsSource()

      // a source of all new effects for a given account
      val effectsForAccountSource = TestNetwork.effectsByAccountSource(publicKey)
      // #effect_source_examples
    }

    def `be present for fee_stats`() = {
      // #fee_stats_query_example
      val feeStats: Future[FeeStatsResponse] = TestNetwork.feeStats()
      val minAcceptedFee: Future[NativeAmount] = feeStats.map(_.minAcceptedFee)
      val percentileFee99: Future[NativeAmount] =
        feeStats.map(_.acceptedFeePercentiles(99))
      // #fee_stats_query_example
    }

    def `be present for payment paths`() = {
      // #payment_paths_query_example
      val payer = KeyPair.fromPassphrase("the payer")
      val payee = KeyPair.fromPassphrase("the payee")
      val usdIssuer = KeyPair.fromPassphrase("the asset issuer")

      val amountToPay = IssuedAmount(5000000, Asset("USD", usdIssuer))
      val paymentPaths: Future[Seq[PaymentPath]] = TestNetwork.paths(
        from = payer,
        to = payee,
        amount = amountToPay
      )
      // #payment_paths_query_example
    }

    def `be present for ledgers`() = {
      // #ledger_query_examples
      // details of a specific ledger
      val ledger: Future[LedgerResponse] = TestNetwork.ledger(1234)

      // stream of all ledgers
      val ledgers: Future[Stream[LedgerResponse]] = TestNetwork.ledgers()

      // stream of the last 20 ledgers
      val last20Ledgers =
        TestNetwork.ledgers(cursor = Now, order = Desc).map(_.take(20))
      // #ledger_query_examples

      // #ledger_source_examples
      // a source of all new ledgers
      val ledgersSource: Source[LedgerResponse, NotUsed] = TestNetwork.ledgersSource()
      // #ledger_source_examples
    }

    def `be present for network info`() = {
      // #network_info_example
      val info: Future[NetworkInfo] = TestNetwork.info()
      val passphrase: Future[String] = info.map(_.passphrase)
      // #network_info_example
    }

    def `be present for offers`() = {
      // #offer_query_examples
      // all offers for a specified account
      val offersByAccount: Future[Stream[OfferResponse]] =
        TestNetwork.offersByAccount(publicKey)

      // most recent offers from a specified account
      val last20Offers = TestNetwork
        .offersByAccount(publicKey, order = Desc, cursor = Now).map(_.take(20))
      // #offer_query_examples

      // #offer_source_examples
      val offersByAccountSource: Source[OfferResponse, NotUsed] =
        TestNetwork.offersByAccountSource(publicKey)
      // #offer_source_examples
    }

    def `be present for operations`() = {
      // #operation_query_examples
      // details of a specific operation
      val operation: Future[Transacted[Operation]] = TestNetwork.operation(1234)

      // stream of all operations
      val operations: Future[Stream[Transacted[Operation]]] = TestNetwork.operations()

      // stream of operations from a specified account
      val opsForAccount: Future[Stream[Transacted[Operation]]] =
        TestNetwork.operationsByAccount(publicKey)

      // stream of operations from a specified ledger
      val opsForLedger: Future[Stream[Transacted[Operation]]] =
        TestNetwork.operationsByLedger(1234)

      // stream of operations from a transaction specified by its hash
      val opsForTxn: Future[Stream[Transacted[Operation]]] =
        TestNetwork.operationsByTransaction("f00cafe...")
      // #operation_query_examples

      // #operation_source_examples
      // a source of all new operations
      val operationsSource: Source[Transacted[Operation], NotUsed] = TestNetwork.operationsSource()

      // a source of all new operations involving a specified account
      val operationsByAccountSource = TestNetwork.operationsByAccountSource(publicKey)
      // #operation_source_examples
    }

    def `be present for orderbooks`() = {
      // #orderbook_query_examples
      // the XLM/HUG orderbook with up to 20 offers
      val hugOrderBook: Future[OrderBook] = TestNetwork.orderBook(
        selling = NativeAsset,
        buying = Asset("HUG", publicKey)
      )

      // the FabulousBeer/HUG orderbook with up to 100 offers
      val beerForHugsBigOrderBook: Future[OrderBook] =
        TestNetwork.orderBook(
          selling = Asset("FabulousBeer", publicKey),
          buying = Asset("HUG", publicKey),
          limit = 100
        )
      // #orderbook_query_examples

      // #orderbook_source_examples
      val beerForHugsBigOrderBookSource: Source[OrderBook, NotUsed] =
        TestNetwork.orderBookSource(
          selling = Asset("FabulousBeer", publicKey),
          buying = Asset("HUG", publicKey),
        )
      // #orderbook_source_examples
    }

    def `be present for payments`() = {
      // #payment_query_examples
      // stream of all payment operations
      val payments: Future[Stream[Transacted[PayOperation]]] = TestNetwork.payments()

      // stream of payment operations involving a specified account
      val accountPayments = TestNetwork.paymentsByAccount(publicKey)

      // stream of payment operations in a specified ledger
      val ledgerPayments = TestNetwork.paymentsByLedger(1234)

      // stream of payment operations in a specified transaction
      val transactionPayments = TestNetwork.paymentsByTransaction("bee042...")
      // #payment_query_examples

      // #payment_source_examples
      // a source of all new payment operations
      val paymentsSource: Source[Transacted[PayOperation], NotUsed] = TestNetwork.paymentsSource()

      // a source of all new payment operations involving a specified account
      val paymentsByAccountSource = TestNetwork.paymentsByAccountSource(publicKey)
      // #payment_source_examples
    }

    def `be present for trades`() = {
      // #trade_query_examples
      // stream of all trades
      val trades: Future[Stream[Trade]] = TestNetwork.trades()

      // stream of trades belonging to a specified orderbook
      val orderBookTrades: Future[Stream[Trade]] = TestNetwork.tradesByOrderBook(
        base = NativeAsset,
        counter = Asset("HUG", publicKey)
      )

      // stream of trades that are created as a result of the specified offer
      val offerBookTrades: Future[Stream[Trade]] = TestNetwork.tradesByOfferId(1234)
      // #trade_query_examples
    }

    def `be present for trade aggregations`() = {
      // #trade_aggregations_examples
      // stream of all trades
      val start = Instant.now().minus(5, DAYS)
      val end = Instant.now()
      val tradeAggregations: Future[Stream[TradeAggregation]] =
        TestNetwork.tradeAggregations(start, end,
          resolution = TradeAggregation.FiveMinutes,
          offsetHours = 0,
          base = NativeAsset,
          counter = Asset("HUG", publicKey)
        )
      // #trade_aggregations_examples
    }

    def `be present for transactions`() = {
      val transactionIdString = "17a670bc424ff5ce3b386dbfaae9990b66a2a37b4fbe51547e8794962a3f9e6a"

      // #transaction_query_examples
      // details of a specific transaction
      val transaction: Future[TransactionHistory] =
        TestNetwork.transaction(transactionIdString)

      // stream of all transactions
      val transactions: Future[Stream[TransactionHistory]] =
        TestNetwork.transactions()

      // stream of transactions affecting the specified account
      val accountTxns = TestNetwork.transactionsByAccount(publicKey)

      // stream of transactions within the specified ledger
      val ledgerTxns = TestNetwork.transactionsByLedger(1234)
      // #transaction_query_examples

      // #transaction_source_examples
      // print each new transaction's hash
      TestNetwork.transactionSource().runForeach(txn => println(txn.hash))

      // a source of transactions for a given account
      val accnTxnSource: Source[TransactionHistory, NotUsed] =
        TestNetwork.transactionsByAccountSource(publicKey)

      // a source of transactions for ledger #3,
      // started from the beginning of time to ensure we get everything
      val ledgerTxnSource = TestNetwork.transactionsByLedgerSource(1, Record(0))
      // #transaction_source_examples
    }
  }

  def `keypair documentation`() = {
    def `show creation of keypair from secret seed`() = {
      // #keypair_from_secret_seed
      // Provide the secret seed as a String
      val keyPair = KeyPair.fromSecretSeed(
        "SDHXK2UNHTXVW2MZSOVOPYUKVXD3PEVKMNQZZGPODQMR67YTKWMOC732")

      // or an Array[Char]
      KeyPair.fromSecretSeed(
        "SDHXK2UNHTXVW2MZSOVOPYUKVXD3PEVKMNQZZGPODQMR67YTKWMOC732".toCharArray)

      // or a raw 32 byte seed
      KeyPair.fromSecretSeed(
        Hex.decodeHex("1123740522f11bfef6b3671f51e159ccf589ccf8965262dd5f97d1721d383dd4")
      )
      // #keypair_from_secret_seed
    }
  }

  //noinspection ScalaUnusedSymbol
  def `transaction documentation`() = {
    implicit val TestNetwork = new DoNothingNetwork

    val Array(sourceKey, aliceKey, bobKey, charlieKey) = Array.fill(4)(
      // #keypair_randomly
      KeyPair.random
      // #keypair_randomly
    )
    val nextSequenceNumber = 1234

    def `show how to create a transaction with operations`() = {
      // #transaction_createwithops_example
      val account = Account(sourceKey, nextSequenceNumber)
      val txn = Transaction(account, Seq(
        CreateAccountOperation(aliceKey),
        CreateAccountOperation(bobKey),
        PaymentOperation(charlieKey, Amount.lumens(42))
      ))
      // #transaction_createwithops_example
    }

    def `show how to add operations afterwards`() = {
      val account = Account(sourceKey, nextSequenceNumber)
      // #transaction_addops_example
      val txn = Transaction(account)
        .add(PaymentOperation(aliceKey, Amount.lumens(100)))
        .add(PaymentOperation(bobKey, Amount.lumens(77)))
        .add(PaymentOperation(charlieKey, Amount.lumens(4.08)))
        .add(CreateSellOfferOperation(
          selling = Amount.lumens(100),
          buying = Asset("FRUITCAKE42", aliceKey),
          price = Price(100, 1)
        ))
      // #transaction_addops_example
    }

    def `show signing`() = {
      val account = Account(sourceKey, nextSequenceNumber)
      val operation = PaymentOperation(aliceKey, Amount.lumens(100))
      // #transaction_signing_example
      val transaction = Transaction(account).add(operation)
      val signedTransaction: SignedTransaction = transaction.sign(sourceKey)
      // #transaction_signing_example
    }

    def `show signing of a joint account`() = {
      val jointAccount = Account(sourceKey, nextSequenceNumber)
      val operation = PaymentOperation(aliceKey, Amount.lumens(100))
      // #joint_transaction_signing_example
      val transaction = Transaction(jointAccount).add(operation)
      val signedTransaction: SignedTransaction = transaction.sign(aliceKey, bobKey)
      // #joint_transaction_signing_example
    }

    def `show submitting`() = {
      val account = Account(sourceKey, nextSequenceNumber)
      val operation = PaymentOperation(aliceKey, Amount.lumens(100))
      // #transaction_submit_example
      val transaction = Transaction(account).add(operation).sign(sourceKey)
      val response: Future[TransactionPostResponse] = transaction.submit()
      // #transaction_submit_example
    }

    def `show checking of response`() = {
      val account = Account(sourceKey, nextSequenceNumber)
      val operation = PaymentOperation(aliceKey, Amount.lumens(100))
      // #transaction_response_example
      Transaction(account).add(operation).sign(sourceKey).submit().foreach {
        case ok: TransactionApproved => println(ok.feeCharged)
        case ko => println(ko)
      }
      // #transaction_response_example
    }
  }

  class DoNothingNetwork extends Network {
    override val passphrase: String = "Scala SDK do-nothing network"
    override val horizon: HorizonAccess = new HorizonAccess {
      override def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResponse] = ???

      override def get[T: ClassTag](path: String, params: Map[String, String])
                                   (implicit ec: ExecutionContext, m: Manifest[T]): Future[T] =
        if (path.endsWith("data/data_key")) {
          Future(DataValueResponse("00").asInstanceOf[T])(ec)
        } else ???

      override def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder, params: Map[String, String] = Map.empty)
                                         (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]] =
        ???

      override def getSource[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, params: Map[String, String])
                                         (implicit ec: ExecutionContext, m: Manifest[T]): Source[T, NotUsed] = Source.empty[T]

      override def getSeq[T: ClassTag](path: String, de: CustomSerializer[T], params: Map[String, String])
                                      (implicit ec: ExecutionContext, m: Manifest[T]): Future[Seq[T]] =
        Future.successful(Seq.empty[T])
    }
  }

  // $COVERAGE-ON$

}

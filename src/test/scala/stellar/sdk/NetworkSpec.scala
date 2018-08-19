package stellar.sdk

import org.json4s.CustomSerializer
import org.scalacheck.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import stellar.sdk.inet._
import stellar.sdk.op._
import stellar.sdk.resp._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class NetworkSpec(implicit ee: ExecutionEnv) extends Specification with ArbitraryInput with Mockito {

  "test network" should {
    "identify itself" >> {
      TestNetwork.passphrase mustEqual "Test SDF Network ; September 2015"
      BigInt(1, TestNetwork.networkId).toString(16).toUpperCase mustEqual
        "CEE0302D59844D32BDCA915C8203DD44B33FBB7EDC19051EA37ABEDF28ECD472"
    }
  }

  "public network" should {
    "identify itself" >> {
      PublicNetwork.passphrase mustEqual "Public Global Stellar Network ; September 2015"
      BigInt(1, PublicNetwork.networkId).toString(16).toUpperCase mustEqual
        "7AC33997544E3175D266BD022439B22CDB16508C01163F26E5CB2A3E1045A979"
    }
  }

  "any network" should {
    "provide access to the master account" >> {
      val standaloneNetworkAccountId = "GBZXN7PIRZGNMHGA7MUUUF4GWPY5AYPV6LY4UV2GL6VJGIQRXFDNMADI"
      StandaloneNetwork.masterAccount.accountId mustEqual standaloneNetworkAccountId
      KeyPair.fromSecretSeed(StandaloneNetwork.masterAccount.secretSeed).accountId  mustEqual standaloneNetworkAccountId
    }

    "submit a signed transaction" >> prop { txn: SignedTransaction =>
      val network = new MockNetwork
      val expected = Future(TransactionPostResp("hash", 1L, "envelopeXDR", "resultXDR", "resultMetaXDR"))
      network.horizon.post(txn) returns expected
      network.submit(txn) mustEqual expected
    }

    "fetch account details by account id" >> prop { pk: PublicKey =>
      val network = new MockNetwork
      val response = mock[AccountResp]
      val expected = Future(response)
      network.horizon.get[AccountResp](s"/accounts/${pk.accountId}") returns expected
      network.account(pk) mustEqual expected
    }

    "fetch data for an account by account id and key" >> prop { (pk: PublicKey, key: String, value: String) =>
      val network = new MockNetwork
      val response = DataValueResp(ByteArrays.base64(value.getBytes("UTF-8")))
      val expected = Future(response)
      network.horizon.get[DataValueResp](s"/accounts/${pk.accountId}/data/$key") returns expected
      network.accountData(pk, key) must beEqualTo(value).await
    }

    "fetch assets by code and/or issuer" >> prop { (code: Option[String], issuer: Option[PublicKey]) =>
      val network = new MockNetwork
      val response = mock[Stream[AssetResp]]
      val expected = Future(response)
      val params = Seq(code.map("asset_code" -> _), issuer.map("asset_issuer" -> _.accountId)).flatten.toMap
      network.horizon.getStream[AssetResp](s"/assets", AssetRespDeserializer, Record(0), Asc, params) returns expected
      network.assets(code, issuer) mustEqual expected
    }

    "fetch descending assets by code and/or issuer" >> prop { (code: Option[String], issuer: Option[PublicKey]) =>
      val network = new MockNetwork
      val response = mock[Stream[AssetResp]]
      val expected = Future(response)
      val params = Seq(code.map("asset_code" -> _), issuer.map("asset_issuer" -> _.accountId)).flatten.toMap
      network.horizon.getStream[AssetResp](s"/assets", AssetRespDeserializer, Record(0), Desc, params) returns expected
      network.assets(code, issuer, Record(0), Desc) mustEqual expected
    }

    "fetch all effects" >> {
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/effects", EffectRespDeserializer, Record(0), Asc) returns expected
      network.effects() mustEqual expected
    }

    "fetch all effects descending" >> {
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/effects", EffectRespDeserializer, Record(0), Desc) returns expected
      network.effects(Record(0), Desc) mustEqual expected
    }

    "fetch effects descending beginning from now" >> {
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/effects", EffectRespDeserializer, Now, Desc) returns expected
      network.effects(Now, Desc) mustEqual expected
    }

    "fetch effects by account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/accounts/${account.accountId}/effects", EffectRespDeserializer, Record(0), Asc) returns expected
      network.effectsByAccount(account) mustEqual expected
    }

    "fetch descending effects by account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/accounts/${account.accountId}/effects", EffectRespDeserializer, Record(0), Desc) returns expected
      network.effectsByAccount(account, Record(0), Desc) mustEqual expected
    }

    "fetch descending effects by account beginning from now" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/accounts/${account.accountId}/effects", EffectRespDeserializer, Now, Desc) returns expected
      network.effectsByAccount(account, Now, Desc) mustEqual expected
    }

    "fetch effects by operation" >> prop { operationId: Long =>
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/operations/$operationId/effects", EffectRespDeserializer, Record(0), Asc) returns expected
      network.effectsByOperation(operationId) mustEqual expected
    }.setGen(Gen.posNum[Long])

    "fetch effects by ledger" >> prop { ledgerId: Long =>
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/ledgers/$ledgerId/effects", EffectRespDeserializer, Record(0), Asc) returns expected
      network.effectsByLedger(ledgerId) mustEqual expected
    }.setGen(Gen.posNum[Long])

    "fetch stream of ledgers" >> {
      val network = new MockNetwork
      val response = mock[Stream[LedgerResp]]
      val expected = Future(response)
      network.horizon.getStream[LedgerResp](s"/ledgers", LedgerRespDeserializer, Record(0), Asc) returns expected
      network.ledgers() mustEqual expected
    }

    "fetch details of a single ledger" >> prop { ledgerId: Long =>
      val network = new MockNetwork
      val response = mock[LedgerResp]
      val expected = Future(response)
      network.horizon.get[LedgerResp](s"/ledgers/$ledgerId") returns expected
      network.ledger(ledgerId) mustEqual expected
    }.setGen(Gen.posNum[Long])

    "fetch effects by transaction hash" >> prop { txnHash: String =>
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/transactions/$txnHash/effects", EffectRespDeserializer, Record(0), Asc) returns expected
      network.effectsByTransaction(txnHash) mustEqual expected
    }.setGen(genHash)

    "fetch offers for an account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[OfferResp]]
      val expected = Future(response)
      network.horizon.getStream[OfferResp](s"/accounts/${account.accountId}/offers", OfferRespDeserializer, Record(0), Asc) returns expected
      network.offersByAccount(account) mustEqual expected
    }

    "fetch offers for an account starting at a defined cursor" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[OfferResp]]
      val expected = Future(response)
      network.horizon.getStream[OfferResp](s"/accounts/${account.accountId}/offers", OfferRespDeserializer, Record(123), Asc) returns expected
      network.offersByAccount(account, Record(123), Asc) mustEqual expected
    }

    "fetch descending offers for an account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[OfferResp]]
      val expected = Future(response)
      network.horizon.getStream[OfferResp](s"/accounts/${account.accountId}/offers", OfferRespDeserializer, Record(0), Desc) returns expected
      network.offersByAccount(account, Record(0), Desc) mustEqual expected
    }

    "fetch descending offers for an account beginning from now" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[OfferResp]]
      val expected = Future(response)
      network.horizon.getStream[OfferResp](s"/accounts/${account.accountId}/offers", OfferRespDeserializer, Now, Desc) returns expected
      network.offersByAccount(account, Now, Desc) mustEqual expected
    }

    "fetch ascending offers for an account beginning from now" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[OfferResp]]
      val expected = Future(response)
      network.horizon.getStream[OfferResp](s"/accounts/${account.accountId}/offers", OfferRespDeserializer, Now, Asc) returns expected
      network.offersByAccount(account, Now, Asc) mustEqual expected
    }

    "fetch details of a single operation" >> prop { id: Long =>
      val network = new MockNetwork
      val response = mock[Transacted[Operation]]
      val expected = Future(response)
      network.horizon.get[Transacted[Operation]](s"/operations/$id") returns expected
      network.operation(id) mustEqual expected
    }

    "fetch a stream of operations" >> {
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/operations", TransactedOperationDeserializer, Record(0), Asc) returns expected
      network.operations(Record(0), Asc) mustEqual expected
    }

    "fetch a descending stream of operations" >> {
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/operations", TransactedOperationDeserializer, Record(0), Desc) returns expected
      network.operations(Record(0), Desc) mustEqual expected
    }

    "fetch a descending stream of operations beginning at cursor 123" >> {
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/operations", TransactedOperationDeserializer, Record(123), Desc) returns expected
      network.operations(Record(123), Desc) mustEqual expected
    }

    "fetch a stream of operations beginning from now" >> {
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/operations", TransactedOperationDeserializer, Now, Asc) returns expected
      network.operations(Now, Asc) mustEqual expected
    }

    "fetch a descending stream of operations beginning from now" >> {
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/operations", TransactedOperationDeserializer, Now, Desc) returns expected
      network.operations(Now, Desc) mustEqual expected
    }

    "fetch a stream of operations filtered by account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/accounts/${account.accountId}/operations", TransactedOperationDeserializer, Record(0), Asc) returns expected
      network.operationsByAccount(account, Record(0), Asc) mustEqual expected
    }

    "fetch a stream of operations filtered by ledger" >> prop { id: Long =>
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/ledgers/$id/operations", TransactedOperationDeserializer, Record(0), Asc) returns expected
      network.operationsByLedger(id, Record(0), Asc) mustEqual expected
    }

    "fetch a stream of operations filtered by transaction" >> prop { hash: String =>
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/transactions/$hash/operations", TransactedOperationDeserializer, Record(0), Asc) returns expected
      network.operationsByTransaction(hash) mustEqual expected
    }.setGen(genHash)

    "fetch the details of the orderbook for an asset pair" >> prop { (buying: Asset, selling: Asset, limit: Int) =>
      val network = new MockNetwork
      val response = mock[OrderBook]
      val expected = Future(response)
      val buyingMap = buying match {
        case NativeAsset => Map("buying_asset_type" -> "native")
        case nna: NonNativeAsset => Map(
          "buying_asset_type" -> nna.typeString,
          "buying_asset_code" -> nna.code,
          "buying_asset_issuer" -> nna.issuer.accountId)
      }
      val sellingMap = selling match {
        case NativeAsset => Map("selling_asset_type" -> "native")
        case nna: NonNativeAsset => Map(
          "selling_asset_type" -> nna.typeString,
          "selling_asset_code" -> nna.code,
          "selling_asset_issuer" -> nna.issuer.accountId)
      }
      val params = Map("limit" -> limit) ++ buyingMap ++ sellingMap
      network.horizon.get[OrderBook]("/order_book", params) returns expected
      network.orderBook(selling, buying, limit) mustEqual expected
    }.setGen3(Gen.posNum[Int])

    "fetch a stream of payment operations" >> {
      val network = new MockNetwork
      val ops = Gen.listOf(genTransacted(genPayOperation)).sample.get
      val expected = Future(ops.map(_.asInstanceOf[Transacted[Operation]]).toStream)
      network.horizon.getStream[Transacted[Operation]](s"/payments", TransactedOperationDeserializer, Record(0), Asc) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.payments() must containTheSameElementsAs(ops).await
    }

    "fetch a stream of payment operations for an account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val ops = Gen.listOf(genTransacted(genPayOperation)).sample.get
      val expected = Future(ops.map(_.asInstanceOf[Transacted[Operation]]).toStream)
      network.horizon.getStream[Transacted[Operation]](s"/accounts/${account.accountId}/payments", TransactedOperationDeserializer, Record(0), Asc) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.paymentsByAccount(account) must containTheSameElementsAs(ops).await
    }

    "fetch a stream of payment operations for a ledger" >> prop { ledgerId: Long =>
      val network = new MockNetwork
      val ops = Gen.listOf(genTransacted(genPayOperation)).sample.get
      val expected = Future(ops.map(_.asInstanceOf[Transacted[Operation]]).toStream)
      network.horizon.getStream[Transacted[Operation]](s"/ledgers/$ledgerId/payments", TransactedOperationDeserializer, Record(0), Asc) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.paymentsByLedger(ledgerId) must containTheSameElementsAs(ops).await
    }

    "fetch a stream of payment operations for a transaction" >> prop { hash: String =>
      val network = new MockNetwork
      val ops = Gen.listOf(genTransacted(genPayOperation)).sample.get
      val expected = Future(ops.map(_.asInstanceOf[Transacted[Operation]]).toStream)
      network.horizon.getStream[Transacted[Operation]](s"/transactions/$hash/payments", TransactedOperationDeserializer, Record(0), Asc) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.paymentsByTransaction(hash) must containTheSameElementsAs(ops).await
    }.setGen(genHash)

    "fetch a stream of trades" >> {
      val network = new MockNetwork
      val response = mock[Stream[Trade]]
      val expected = Future(response)
      network.horizon.getStream[Trade]("/trades", TradeDeserializer, Record(0), Asc) returns expected
      network.trades() mustEqual expected
    }

    "fetch a stream of trades filtered by orderbook" >> prop { (base: Asset, counter: Asset) =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[Trade]])
      val buyingMap = base match {
        case NativeAsset => Map("base_asset_type" -> "native")
        case nna: NonNativeAsset => Map(
          "base_asset_type" -> nna.typeString,
          "base_asset_code" -> nna.code,
          "base_asset_issuer" -> nna.issuer.accountId)
      }
      val sellingMap = counter match {
        case NativeAsset => Map("counter_asset_type" -> "native")
        case nna: NonNativeAsset => Map(
          "counter_asset_type" -> nna.typeString,
          "counter_asset_code" -> nna.code,
          "counter_asset_issuer" -> nna.issuer.accountId)
      }
      val params = buyingMap ++ sellingMap
      network.horizon.getStream[Trade]("/trades", TradeDeserializer, Record(0), Asc, params) returns expected
      network.tradesByOrderBook(base, counter) mustEqual expected
    }

    "fetch a stream of trades filtered by offer id" >> prop { offerId: Long =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[Trade]])
      val params = Map("offerid" -> s"$offerId")
      network.horizon.getStream[Trade]("/trades", TradeDeserializer, Record(0), Asc, params) returns expected
      network.tradesByOfferId(offerId, Record(0), Asc) mustEqual expected
    }

    "fetch a descending stream of trades filtered by offer id" >> prop { offerId: Long =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[Trade]])
      val params = Map("offerid" -> s"$offerId")
      network.horizon.getStream[Trade]("/trades", TradeDeserializer, Record(0), Desc, params) returns expected
      network.tradesByOfferId(offerId, Record(0), Desc) mustEqual expected
    }

    "fetch a descending stream of trades filtered by offer id beginning from now" >> prop { offerId: Long =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[Trade]])
      val params = Map("offerid" -> s"$offerId")
      network.horizon.getStream[Trade]("/trades", TradeDeserializer, Now, Desc, params) returns expected
      network.tradesByOfferId(offerId, Now, Desc) mustEqual expected
    }

    "fetch an ascending stream of trades filtered by offer id beginning from now" >> prop { offerId: Long =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[Trade]])
      val params = Map("offerid" -> s"$offerId")
      network.horizon.getStream[Trade]("/trades", TradeDeserializer, Now, Asc, params) returns expected
      network.tradesByOfferId(offerId, Now, Asc) mustEqual expected
    }

    "fetch a stream of transactions" >> {
      val network = new MockNetwork
      val expected = Future(mock[Stream[TransactionHistoryResp]])
      network.horizon.getStream[TransactionHistoryResp]("/transactions", TransactionHistoryRespDeserializer, Record(0), Asc) returns expected
      network.transactions() mustEqual expected
    }

    "fetch a stream of transactions for a given account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[TransactionHistoryResp]])
      network.horizon.getStream[TransactionHistoryResp](s"/accounts/${account.accountId}/transactions", TransactionHistoryRespDeserializer, Record(0), Asc) returns expected
      network.transactionsByAccount(account) mustEqual expected
    }

    "fetch a stream of transactions for a given ledger" >> prop { ledgeId: Long =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[TransactionHistoryResp]])
      network.horizon.getStream[TransactionHistoryResp](s"/ledgers/$ledgeId/transactions", TransactionHistoryRespDeserializer, Record(0), Asc) returns expected
      network.transactionsByLedger(ledgeId) mustEqual expected
    }
  }

  //noinspection ScalaUnusedSymbol
  // $COVERAGE-OFF$
  "query documentation" should {
    val TestNetwork = new DoNothingNetwork
    val accountId = "GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"
    val publicKey = KeyPair.fromAccountId(accountId)

    "be present for accounts" >> {
      // #account_query_examples
      val accountId = "GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"
      val publicKey = KeyPair.fromAccountId(accountId)

      // account details
      val accountDetails: Future[AccountResp] = TestNetwork.account(publicKey)

      // account datum value
      val accountData: Future[String] = TestNetwork.accountData(publicKey, "data_key")
      // #account_query_examples

      ok
    }

    "be present for assets" >> {
      // #asset_query_examples
      // stream of all assets from all issuers
      val allAssets: Future[Stream[AssetResp]] = TestNetwork.assets()

      // stream of the last 20 assets created
      val last20Assets =
        TestNetwork.assets(cursor = Now, order = Desc).map(_.take(20))

      // stream of assets with the code HUG
      val hugAssets: Future[Stream[AssetResp]] = TestNetwork.assets(code = Some("HUG"))

      // stream of assets from the specified issuer
      val issuerAssets: Future[Stream[AssetResp]] =
        TestNetwork.assets(issuer = Some(publicKey))

      // Stream (of max length 1) of HUG assets from the issuer
      val issuersHugAsset: Future[Stream[AssetResp]] =
        TestNetwork.assets(code = Some("HUG"), issuer = Some(publicKey))
      // #asset_query_examples
      ok
    }

    "be present for effects" >> {
      // #effect_query_examples
      // stream of all effects
      val allEffects: Future[Stream[EffectResp]] = TestNetwork.effects()

      // stream of the last 20 effects
      val last20Effects =
        TestNetwork.effects(cursor = Now, order = Desc).map(_.take(20))

      // stream of effects related to a specific account
      val effectsForAccount = TestNetwork.effectsByAccount(publicKey)

      // stream of effects related to a specific transaction hash
      val effectsForTxn: Future[Stream[EffectResp]] =
        TestNetwork.effectsByTransaction("f00cafe...")

      // stream of effects related to a specific operation id
      val effectsForOperationId: Future[Stream[EffectResp]] =
        TestNetwork.effectsByOperation(123L)

      // stream of effects for a specific ledger
      val effectsForLedger = TestNetwork.effectsByLedger(1234)
      // #effect_query_examples
      ok
    }

    "be present for ledgers" >> {
      // #ledger_query_examples
      // details of a specific ledger
      val ledger: Future[LedgerResp] = TestNetwork.ledger(1234)

      // stream of all ledgers
      val ledgers: Future[Stream[LedgerResp]] = TestNetwork.ledgers()

      // stream of the last 20 ledgers
      val last20Ledgers =
        TestNetwork.ledgers(cursor = Now, order = Desc).map(_.take(20))
      // #ledger_query_examples
      ok
    }

    "be present for offers" >> {
      // #offer_query_examples
      // all offers for a specified account
      val offersByAccount: Future[Stream[OfferResp]] =
        TestNetwork.offersByAccount(publicKey)

      // most recent offers from a specified account
      val last20Offers = TestNetwork
        .offersByAccount(publicKey, order = Desc, cursor = Now).map(_.take(20))
      // #offer_query_examples
      ok
    }

    "be present for operations" >> {
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

      ok
    }

    "be present for orderbooks" >> {
      // #orderbook_query_examples
      // the XLM/HUG orderbook with up to 20 offers
      val hugOrderBook: Future[OrderBook] = TestNetwork.orderBook(
        selling = NativeAsset,
        buying = Asset("HUG", publicKey)
      )

      // the FabulousBeer/HUG orderbook with up to 100 offers
      val beerForHugsBigOrderBook: Future[OrderBook] = TestNetwork.orderBook(
        selling = Asset("FabulousBeer", publicKey),
        buying = Asset("HUG", publicKey),
        limit = 100
      )
      // #orderbook_query_examples
      ok
    }

    "be present for payments" >> {
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
      ok
    }

    "be present for trades" >> {
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
      ok
    }

    "be present for transactions" >> {
      // #transaction_query_examples
      // stream of all transactions
      val transactions: Future[Stream[TransactionHistoryResp]] =
        TestNetwork.transactions()

      // stream of transactions affecting the specified account
      val accountTxns = TestNetwork.transactionsByAccount(publicKey)

      // stream of transactions within the specified ledger
      val ledgerTxns = TestNetwork.transactionsByLedger(1234)
      // #transaction_query_examples
      ok
    }
  }

  //noinspection ScalaUnusedSymbol
  "transaction documentation" should {
    val Array(sourceKey, aliceKey, bobKey, charlieKey) = Array.fill(4)(KeyPair.random)
    val nextSequenceNumber = 1234

    "show how to create a transaction with operations" >> {
      // #transaction_createwithops_example
      val account = Account(sourceKey, nextSequenceNumber)
      val txn = Transaction(account, Seq(
        CreateAccountOperation(aliceKey),
        CreateAccountOperation(bobKey),
        PaymentOperation(charlieKey, Amount.lumens(42))
      ))
      // #transaction_createwithops_example
      ok
    }

    "show how to add operations afterwards" >> {
      val account = Account(sourceKey, nextSequenceNumber)
      // #transaction_addops_example
      val txn = Transaction(account)
        .add(PaymentOperation(aliceKey, Amount.lumens(100)))
        .add(PaymentOperation(bobKey, Amount.lumens(77)))
        .add(PaymentOperation(charlieKey, Amount.lumens(4.08)))
        .add(CreateOfferOperation(
          selling = Amount.lumens(100),
          buying = Asset("FRUITCAKE42", aliceKey),
          price = Price(100, 1)
        ))
      // #transaction_addops_example
      ok
    }

    "show signing" >> {
      val account = Account(sourceKey, nextSequenceNumber)
      val operation = PaymentOperation(aliceKey, Amount.lumens(100))
      // #transaction_signing_example
      val transaction = Transaction(account).add(operation)
      val signedTransaction: SignedTransaction = transaction.sign(sourceKey)
      // #transaction_signing_example
      ok
    }

    "show signing of a joint account" >> {
      val jointAccount = Account(sourceKey, nextSequenceNumber)
      val operation = PaymentOperation(aliceKey, Amount.lumens(100))
      // #joint_transaction_signing_example
      val transaction = Transaction(jointAccount).add(operation)
      val signedTransaction: SignedTransaction = transaction.sign(aliceKey, bobKey)
      // #joint_transaction_signing_example
      ok
    }

    "show submitting" >> {
      val account = Account(sourceKey, nextSequenceNumber)
      val operation = PaymentOperation(aliceKey, Amount.lumens(100))
      // #transaction_submit_example
      val transaction = Transaction(account).add(operation).sign(sourceKey)
      val response: Future[TransactionPostResp] = transaction.submit()
      // #transaction_submit_example
      ok
    }

    "show checking of response" >> {
      val account = Account(sourceKey, nextSequenceNumber)
      val operation = PaymentOperation(aliceKey, Amount.lumens(100))
      // #transaction_response_example
      Transaction(account).add(operation).sign(sourceKey).submit().foreach {
        response => println(response.result.getFeeCharged)
      }
      // #transaction_response_example
      ok
    }
  }
  // $COVERAGE-ON$

  class MockNetwork extends Network {
    override val passphrase: String = "Scala SDK mock network"
    override val horizon: HorizonAccess = mock[HorizonAccess]
  }

  class DoNothingNetwork extends Network {
    override val passphrase: String = "Scala SDK do-nothing network"
    override val horizon: HorizonAccess = new HorizonAccess {
      override def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResp] =
        mock[Future[TransactionPostResp]]

      override def get[T: ClassTag](path: String, params: Map[String, Any])
                                   (implicit ec: ExecutionContext, m: Manifest[T]): Future[T] =
        if (path.endsWith("data/data_key")) {
          Future(DataValueResp("00").asInstanceOf[T])(ec)
        } else {
          mock[Future[T]]
        }

      override def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder, params: Map[String, String] = Map.empty)
                                         (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]] =
        mock[Future[Stream[T]]]

      override def getPage[T: ClassTag](path: String, params: Map[String, String])
                                       (implicit ec: ExecutionContext, de: CustomSerializer[T], m: Manifest[T]): Future[Page[T]] =
        mock[Future[Page[T]]]
    }
  }
}

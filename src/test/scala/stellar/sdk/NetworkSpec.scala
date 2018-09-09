package stellar.sdk

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import org.scalacheck.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import stellar.sdk.inet._
import stellar.sdk.op._
import stellar.sdk.resp._

import scala.concurrent.Future
import scala.concurrent.duration._

class NetworkSpec(implicit ee: ExecutionEnv) extends Specification with ArbitraryInput with Mockito {

  implicit val system = ActorSystem("network-spec")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

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
      val networkAccountId = "GBRPYHIL2CI3FNQ4BXLFMNDLFJUNPU2HY3ZMFSHONUCEOASW7QC7OX2H"
      TestNetwork.masterAccount.accountId mustEqual networkAccountId
      KeyPair.fromSecretSeed(TestNetwork.masterAccount.secretSeed).accountId  mustEqual networkAccountId
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

    "provide source of all effects" >> prop { cursor: HorizonCursor =>
      val network = new MockNetwork
      val op = mock[EffectResp]
      val expectedSource: Source[EffectResp, NotUsed] = Source.fromFuture(Future(op))
      network.horizon.getSource("/effects", EffectRespDeserializer, cursor) returns expectedSource
      network.effectsSource(cursor).toMat(Sink.seq)(Keep.right).run must beEqualTo(Seq(op)).awaitFor(10.seconds)
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

    "provide source of all effects filtered by account" >> prop { (account: PublicKey, cursor: HorizonCursor) =>
      val network = new MockNetwork
      val op = mock[EffectResp]
      val expectedSource: Source[EffectResp, NotUsed] = Source.fromFuture(Future(op))
      network.horizon.getSource(s"/accounts/${account.accountId}/effects", EffectRespDeserializer, cursor) returns expectedSource
      network.effectsByAccountSource(account, cursor).toMat(Sink.seq)(Keep.right).run must beEqualTo(Seq(op)).awaitFor(10.seconds)
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

    "provide a source of ledgers" >> prop { cursor: HorizonCursor =>
      val network = new MockNetwork
      val op = mock[LedgerResp]
      val expectedSource: Source[LedgerResp, NotUsed] = Source.fromFuture(Future(op))
      network.horizon.getSource("/ledgers", LedgerRespDeserializer, cursor) returns expectedSource
      network.ledgersSource(cursor).toMat(Sink.seq)(Keep.right).run must beEqualTo(Seq(op)).awaitFor(10.seconds)
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

    "provide a source of offers for an account" >> prop { (account: PublicKey, cursor: HorizonCursor) =>
      val network = new MockNetwork
      val op = mock[OfferResp]
      val expectedSource: Source[OfferResp, NotUsed] = Source.fromFuture(Future(op))
      network.horizon.getSource(s"/accounts/${account.accountId}/offers", OfferRespDeserializer, cursor) returns expectedSource
      network.offersByAccountSource(account, cursor).toMat(Sink.seq)(Keep.right).run must beEqualTo(Seq(op)).awaitFor(10.seconds)
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

    "provide a source of operations" >> prop { cursor: HorizonCursor =>
      val network = new MockNetwork
      val op = mock[Transacted[Operation]]
      val expectedSource: Source[Transacted[Operation], NotUsed] = Source.fromFuture(Future(op))
      network.horizon.getSource(s"/operations", TransactedOperationDeserializer, cursor) returns expectedSource
      network.operationsSource(cursor).toMat(Sink.seq)(Keep.right).run must beEqualTo(Seq(op)).awaitFor(10.seconds)
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

    "provide a source of operations for an account" >> prop { (account: PublicKey, cursor: HorizonCursor) =>
      val network = new MockNetwork
      val op = mock[Transacted[Operation]]
      val expectedSource: Source[Transacted[Operation], NotUsed] = Source.fromFuture(Future(op))
      network.horizon.getSource(s"/accounts/${account.accountId}/operations", TransactedOperationDeserializer, cursor) returns expectedSource
      network.operationsByAccountSource(account, cursor).toMat(Sink.seq)(Keep.right).run must beEqualTo(Seq(op)).awaitFor(10.seconds)
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
      val params = Map("limit" -> limit.toString) ++ buyingMap ++ sellingMap
      network.horizon.get[OrderBook]("/order_book", params) returns expected
      network.orderBook(selling, buying, limit) mustEqual expected
    }.setGen3(Gen.posNum[Int])

    "provide a source of order books"  >> prop { (buying: Asset, selling: Asset, cursor: HorizonCursor) =>
      val network = new MockNetwork
      val expected = Source.empty[OrderBook]
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
      val params = buyingMap ++ sellingMap
      network.horizon.getSource("/order_book", OrderBookDeserializer, cursor, params) returns expected
      network.orderBookSource(selling, buying, cursor) mustEqual expected
    }

    "fetch a stream of payment operations" >> {
      val network = new MockNetwork
      val ops = Gen.listOf(genTransacted(genPayOperation)).sample.get
      val expected = Future(ops.map(_.asInstanceOf[Transacted[Operation]]).toStream)
      network.horizon.getStream[Transacted[Operation]](s"/payments", TransactedOperationDeserializer, Record(0), Asc) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.payments() must containTheSameElementsAs(ops).await
    }

    "provide a source of payment operations" >> prop { cursor: HorizonCursor =>
      val network = new MockNetwork
      val op = mock[Transacted[PaymentOperation]]
      val expectedSource: Source[Transacted[Operation], NotUsed] = Source.fromFuture(Future(op.asInstanceOf[Transacted[Operation]]))
      network.horizon.getSource("/payments", TransactedOperationDeserializer, cursor) returns expectedSource
      network.paymentsSource(cursor).toMat(Sink.seq)(Keep.right).run must beEqualTo(Seq(op)).awaitFor(10.seconds)
    }

    "fetch a stream of payment operations for an account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val ops = Gen.listOf(genTransacted(genPayOperation)).sample.get
      val expected = Future(ops.map(_.asInstanceOf[Transacted[Operation]]).toStream)
      network.horizon.getStream[Transacted[Operation]](s"/accounts/${account.accountId}/payments", TransactedOperationDeserializer, Record(0), Asc) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.paymentsByAccount(account) must containTheSameElementsAs(ops).await
    }

    "provide a source of payment operations for an account" >> prop { (account: PublicKey, cursor: HorizonCursor) =>
      val network = new MockNetwork
      val op = mock[Transacted[PaymentOperation]]
      val expectedSource: Source[Transacted[Operation], NotUsed] = Source.fromFuture(Future(op.asInstanceOf[Transacted[Operation]]))
      network.horizon.getSource(s"/accounts/${account.accountId}/payments", TransactedOperationDeserializer, cursor) returns expectedSource
      network.paymentsByAccountSource(account, cursor).toMat(Sink.seq)(Keep.right).run must beEqualTo(Seq(op)).awaitFor(10.seconds)
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

    "fetch a stream of transactions for a given ledger" >> prop { ledgerId: Long =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[TransactionHistoryResp]])
      network.horizon.getStream[TransactionHistoryResp](s"/ledgers/$ledgerId/transactions", TransactionHistoryRespDeserializer, Record(0), Asc) returns expected
      network.transactionsByLedger(ledgerId) mustEqual expected
    }

    "provide a source of transactions" >> prop { cursor: HorizonCursor =>
      val network = new MockNetwork
      val expected = Source.empty[TransactionHistoryResp]
      network.horizon.getSource("/transactions", TransactionHistoryRespDeserializer, cursor) returns expected
      network.transactionSource(cursor) mustEqual expected
    }

    "provide a source of transactions by account" >> prop { (pk: PublicKey, cursor: HorizonCursor) =>
      val network = new MockNetwork
      val expected = Source.empty[TransactionHistoryResp]
      network.horizon.getSource(s"/accounts/${pk.accountId}/transactions", TransactionHistoryRespDeserializer, cursor) returns expected
      network.transactionsByAccountSource(pk, cursor) mustEqual expected
    }

    "provide a source of transactions by ledger" >> prop { ledgerId: Long =>
      val network = new MockNetwork
      val expected = Source.empty[TransactionHistoryResp]
      network.horizon.getSource(s"/ledgers/$ledgerId/transactions", TransactionHistoryRespDeserializer, Now) returns expected
      network.transactionsByLedgerSource(ledgerId) mustEqual expected
    }
  }

  class MockNetwork extends Network {
    override val passphrase: String = "Scala SDK mock network"
    override val horizon: HorizonAccess = mock[HorizonAccess]
  }

}

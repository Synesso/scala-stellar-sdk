package stellar.sdk

import org.scalacheck.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import stellar.sdk.inet.HorizonAccess
import stellar.sdk.op._
import stellar.sdk.resp._

import scala.concurrent.Future

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
      network.horizon.getStream[AssetResp](s"/assets", AssetRespDeserializer, params) returns expected
      network.assets(code, issuer) mustEqual expected
    }

    "fetch all effects" >> {
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/effects", EffectRespDeserializer) returns expected
      network.effects() mustEqual expected
    }

    "fetch effects by account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/accounts/${account.accountId}/effects", EffectRespDeserializer) returns expected
      network.effectsByAccount(account) mustEqual expected
    }

    "fetch effects by ledger" >> prop { ledgerId: Long =>
      val network = new MockNetwork
      val response = mock[Stream[EffectResp]]
      val expected = Future(response)
      network.horizon.getStream[EffectResp](s"/ledgers/$ledgerId/effects", EffectRespDeserializer) returns expected
      network.effectsByLedger(ledgerId) mustEqual expected
    }.setGen(Gen.posNum[Long])

    "fetch stream of ledgers" >> {
      val network = new MockNetwork
      val response = mock[Stream[LedgerResp]]
      val expected = Future(response)
      network.horizon.getStream[LedgerResp](s"/ledgers", LedgerRespDeserializer) returns expected
      network.ledgers() mustEqual expected
    }

    "fetch details of a single ledger" >> prop { ledgerId: Long =>
      val network = new MockNetwork
      val response = mock[LedgerResp]
      val expected = Future(response)
      network.horizon.get[LedgerResp](s"/ledgers/$ledgerId") returns expected
      network.ledger(ledgerId) mustEqual expected
    }.setGen(Gen.posNum[Long])

    "fetch offers for an account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[OfferResp]]
      val expected = Future(response)
      network.horizon.getStream[OfferResp](s"/accounts/${account.accountId}/offers", OfferRespDeserializer) returns expected
      network.offersByAccount(account) mustEqual expected
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
      network.horizon.getStream[Transacted[Operation]](s"/operations", TransactedOperationDeserializer) returns expected
      network.operations() mustEqual expected
    }

    "fetch a stream of operations filtered by account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/accounts/${account.accountId}/operations", TransactedOperationDeserializer) returns expected
      network.operationsByAccount(account) mustEqual expected
    }

    "fetch a stream of operations filtered by ledger" >> prop { id: Long =>
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/ledgers/$id/operations", TransactedOperationDeserializer) returns expected
      network.operationsByLedger(id) mustEqual expected
    }

    "fetch a stream of operations filtered by transaction" >> prop { hash: String =>
      val network = new MockNetwork
      val response = mock[Stream[Transacted[Operation]]]
      val expected = Future(response)
      network.horizon.getStream[Transacted[Operation]](s"/transactions/$hash/operations", TransactedOperationDeserializer) returns expected
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
      network.horizon.getStream[Transacted[Operation]](s"/payments", TransactedOperationDeserializer) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.payments() must containTheSameElementsAs(ops).await
    }

    "fetch a stream of payment operations for an account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val ops = Gen.listOf(genTransacted(genPayOperation)).sample.get
      val expected = Future(ops.map(_.asInstanceOf[Transacted[Operation]]).toStream)
      network.horizon.getStream[Transacted[Operation]](s"/accounts/${account.accountId}/payments", TransactedOperationDeserializer) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.paymentsByAccount(account) must containTheSameElementsAs(ops).await
    }

    "fetch a stream of payment operations for a ledger" >> prop { ledgerId: Long =>
      val network = new MockNetwork
      val ops = Gen.listOf(genTransacted(genPayOperation)).sample.get
      val expected = Future(ops.map(_.asInstanceOf[Transacted[Operation]]).toStream)
      network.horizon.getStream[Transacted[Operation]](s"/ledgers/$ledgerId/payments", TransactedOperationDeserializer) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.paymentsByLedger(ledgerId) must containTheSameElementsAs(ops).await
    }

    "fetch a stream of payment operations for a transaction" >> prop { hash: String =>
      val network = new MockNetwork
      val ops = Gen.listOf(genTransacted(genPayOperation)).sample.get
      val expected = Future(ops.map(_.asInstanceOf[Transacted[Operation]]).toStream)
      network.horizon.getStream[Transacted[Operation]](s"/transactions/$hash/payments", TransactedOperationDeserializer) returns
        expected.asInstanceOf[Future[Stream[Transacted[Operation]]]]
      network.paymentsByTransaction(hash) must containTheSameElementsAs(ops).await
    }.setGen(genHash)

    "fetch a stream of trades" >> {
      val network = new MockNetwork
      val response = mock[Stream[Trade]]
      val expected = Future(response)
      network.horizon.getStream[Trade]("/trades", TradeDeserializer) returns expected
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
      network.horizon.getStream[Trade]("/trades", TradeDeserializer, params) returns expected
      network.tradesByOrderBook(base, counter) mustEqual expected
    }

    "fetch a stream of trades filtered by offer id" >> prop { offerId: Long =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[Trade]])
      val params = Map("offerid" -> s"$offerId")
      network.horizon.getStream[Trade]("/trades", TradeDeserializer, params) returns expected
      network.tradesByOfferId(offerId) mustEqual expected
    }

    "fetch a stream of transactions" >> {
      val network = new MockNetwork
      val expected = Future(mock[Stream[TransactionHistoryResp]])
      network.horizon.getStream[TransactionHistoryResp]("/transactions", TransactionHistoryRespDeserializer) returns expected
      network.transactions() mustEqual expected
    }

    "fetch a stream of transactions for a given account" >> prop { account: PublicKey =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[TransactionHistoryResp]])
      network.horizon.getStream[TransactionHistoryResp](s"/accounts/${account.accountId}/transactions", TransactionHistoryRespDeserializer) returns expected
      network.transactionsByAccount(account) mustEqual expected
    }

    "fetch a stream of transactions for a given ledger" >> prop { ledgeId: Long =>
      val network = new MockNetwork
      val expected = Future(mock[Stream[TransactionHistoryResp]])
      network.horizon.getStream[TransactionHistoryResp](s"/ledgers/$ledgeId/transactions", TransactionHistoryRespDeserializer) returns expected
      network.transactionsByLedger(ledgeId) mustEqual expected
    }

//    "example of creating and submitting a payment transaction" >> {
//      val secretSeed = "".getBytes()
//      val transactionSource = KeyPair.fromSecretSeed(seed)
//      val txn = Transaction()
//    }

  }

  class MockNetwork extends Network {
    override val passphrase: String = "Scala SDK mock network"
    override val horizon: HorizonAccess = mock[HorizonAccess]
  }
}


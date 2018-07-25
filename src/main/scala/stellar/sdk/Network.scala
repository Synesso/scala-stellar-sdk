package stellar.sdk

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

import com.typesafe.scalalogging.LazyLogging
import stellar.sdk.ByteArrays._
import stellar.sdk.inet.{Horizon, HorizonAccess}
import stellar.sdk.op.{Operation, PayOperation, Transacted, TransactedOperationDeserializer}
import stellar.sdk.resp._

import scala.concurrent.{ExecutionContext, Future}

trait Network extends LazyLogging {
  val passphrase: String
  lazy val networkId: Array[Byte] = sha256(passphrase.getBytes(UTF_8)).get
  val horizon: HorizonAccess

  /**
    * Submit the SignedTransaction to the network and eventually receive a TransactionPostResp with the results.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/transactions-create.html
    */
  def submit(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResp] = horizon.post(txn)

  /**
    * Fetch details regarding the account identified by `pubKey`.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/accounts-single.html
    */
  def account(pubKey: PublicKeyOps)(implicit ec: ExecutionContext): Future[AccountResp] =
    horizon.get[AccountResp](s"/accounts/${pubKey.accountId}")

  /**
    * Fetch value for single data field associated with an account.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/data-for-account.html
    */
  def accountData(pubKey: PublicKeyOps, dataKey: String)(implicit ec: ExecutionContext): Future[String] =
    horizon.get[DataValueResp](s"/accounts/${pubKey.accountId}/data/$dataKey").map(_.v).map(base64).map(new String(_))

  /**
    * Fetch a stream of assets, optionally filtered by code, issuer or neither
    * @param code optional code to filter by
    * @param issuer optional issuer account to filter by
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/assets-all.html
    */
  def assets(code: Option[String] = None, issuer: Option[PublicKeyOps] = None)(implicit ec: ExecutionContext): Future[Stream[AssetResp]] = {
    val params = Seq(code.map("asset_code" -> _), issuer.map("asset_issuer" -> _.accountId)).flatten.toMap
    horizon.getStream[AssetResp](s"/assets", AssetRespDeserializer, params)
  }

  /**
    * Fetch a stream of effects.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/effects-all.html
    */
  def effects()(implicit ec: ExecutionContext): Future[Stream[EffectResp]] =
    horizon.getStream[EffectResp]("/effects", EffectRespDeserializer)

  /**
    * Fetch a stream of effects for a given account.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-account.html
    */
  def effectsByAccount(account: PublicKeyOps)(implicit ec: ExecutionContext): Future[Stream[EffectResp]] =
    horizon.getStream[EffectResp](s"/accounts/${account.accountId}/effects", EffectRespDeserializer)

  /**
    * Fetch a stream of effects for a given ledger.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-ledger.html
    */
  def effectsByLedger(ledgerId: Long)(implicit ec: ExecutionContext): Future[Stream[EffectResp]] =
    horizon.getStream[EffectResp](s"/ledgers/$ledgerId/effects", EffectRespDeserializer)

  /**
    * Fetch a stream of details about ledgers.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/ledgers-all.html
    */
  def ledgers()(implicit ec: ExecutionContext): Future[Stream[LedgerResp]] =
    horizon.getStream[LedgerResp](s"/ledgers", LedgerRespDeserializer)

  /**
    * Fetch details of a ledger by its id
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/ledgers-single.html
    */
  def ledger(ledgerId: Long)(implicit ex: ExecutionContext): Future[LedgerResp] =
    horizon.get[LedgerResp](s"/ledgers/$ledgerId")

  /**
    * Fetch a stream of offers for an account.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/offers-for-account.html
    */
  def offersByAccount(account: PublicKeyOps)(implicit ex: ExecutionContext): Future[Stream[OfferResp]] =
    horizon.getStream[OfferResp](s"/accounts/${account.accountId}/offers", OfferRespDeserializer)

  /**
    * Fetch operation details by its id
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/operations-single.html
    */
  def operation(operationId: Long)(implicit ex: ExecutionContext): Future[Transacted[Operation]] =
    horizon.get[Transacted[Operation]](s"/operations/$operationId")

  /**
    * Fetch a stream of operations.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/operations-all.html
    */
  def operations()(implicit ex: ExecutionContext): Future[Stream[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"/operations", TransactedOperationDeserializer)

  /**
    * Fetch a stream of operations, filtered by account.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-account.html
    */
  def operationsByAccount(pubKey: PublicKeyOps)(implicit ex: ExecutionContext): Future[Stream[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"/accounts/${pubKey.accountId}/operations", TransactedOperationDeserializer)

  /**
    * Fetch a stream of operations, filtered by ledger id.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-ledger.html
    */
  def operationsByLedger(ledgerId: Long)(implicit ex: ExecutionContext): Future[Stream[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"/ledgers/$ledgerId/operations", TransactedOperationDeserializer)

  /**
    * Fetch a stream of operations, filtered by transaction hash.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-transaction.html
    */
  def operationsByTransaction(txnHash: String)(implicit ex: ExecutionContext): Future[Stream[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"/transactions/$txnHash/operations", TransactedOperationDeserializer)

  /**
    * Fetch details of the current orderbook for the given asset pairs
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/orderbook-details.html
    */
  def orderBook(selling: Asset, buying: Asset, limit: Int = 20)(implicit ex: ExecutionContext): Future[OrderBook] = {
    val params = assetParams("selling", selling) ++ assetParams("buying", buying).updated("limit", limit)
    horizon.get[OrderBook]("/order_book", params)
  }

  /**
    * Fetch a stream of payment operations.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/payments-all.html
    */
  def payments()(implicit ex: ExecutionContext): Future[Stream[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"/payments", TransactedOperationDeserializer)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of payment operations filtered by account.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/payments-for-account.html
    */
  def paymentsByAccount(pubKey: PublicKeyOps)(implicit ex: ExecutionContext): Future[Stream[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"/accounts/${pubKey.accountId}/payments", TransactedOperationDeserializer)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of payment operations filtered by ledger id.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/payments-for-ledger.html
    */
  def paymentsByLedger(ledgerId: Long)(implicit ex: ExecutionContext): Future[Stream[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"/ledgers/$ledgerId/payments", TransactedOperationDeserializer)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of payment operations filtered by transaction hash.
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-transaction.html
    */
  def paymentsByTransaction(txnHash: String)(implicit ex: ExecutionContext): Future[Stream[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"/transactions/$txnHash/payments", TransactedOperationDeserializer)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of trades
    * @see https://www.stellar.org/developers/horizon/reference/endpoints/trades.html
    */
  def trades()(implicit ex: ExecutionContext): Future[Stream[Trade]] =
    horizon.getStream[Trade]("/trades", TradeDeserializer)

  def tradesByOrderBook(base: Asset, counter: Asset)(implicit ex: ExecutionContext): Future[Stream[Trade]] = {
    val params = assetParams("base", base) ++ assetParams("counter", counter)
    horizon.getStream[Trade]("/trades", TradeDeserializer, params)
  }

  def tradesByOfferId(offerId: Long)(implicit ex: ExecutionContext): Future[Stream[Trade]] = {
    val params = Map("offerid" -> s"$offerId")
    horizon.getStream[Trade]("/trades", TradeDeserializer, params)
  }

  def transactions()(implicit ex: ExecutionContext): Future[Stream[TransactionHistoryResp]] = {
    horizon.getStream[TransactionHistoryResp]("/transactions", TransactionHistoryRespDeserializer)
  }

  def transactionsByAccount(pubKey: PublicKeyOps)(implicit ex: ExecutionContext): Future[Stream[TransactionHistoryResp]] = {
    horizon.getStream[TransactionHistoryResp](s"/accounts/${pubKey.accountId}/transactions", TransactionHistoryRespDeserializer)
  }

  def transactionsByLedger(sequenceId: Long)(implicit ex: ExecutionContext): Future[Stream[TransactionHistoryResp]] = {
    horizon.getStream[TransactionHistoryResp](s"/ledgers/$sequenceId/transactions", TransactionHistoryRespDeserializer)
  }

  private def assetParams(prefix: String, asset: Asset): Map[String, String] = {
    asset match {
      case NativeAsset => Map(s"${prefix}_asset_type" -> "native")
      case nna: NonNativeAsset => Map(
        s"${prefix}_asset_type" -> nna.typeString,
        s"${prefix}_asset_code" -> nna.code,
        s"${prefix}_asset_issuer" -> nna.issuer.accountId
      )
    }
  }

}

case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
  val horizon = new Horizon(URI.create("https://horizon.stellar.org"))
}

case object TestNetwork extends Network {
  override val passphrase = "Test SDF Network ; September 2015"
  val horizon = new Horizon(URI.create("https://horizon-testnet.stellar.org"))

  def fund(pk: PublicKeyOps)(implicit ec: ExecutionContext): Future[TransactionPostResp] =
    horizon.get[TransactionPostResp]("/friendbot", Map("addr" -> pk.accountId))
}

package stellar.sdk

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import stellar.sdk.ByteArrays._
import stellar.sdk.inet._
import stellar.sdk.op.{Operation, PayOperation, Transacted, TransactedOperationDeserializer}
import stellar.sdk.resp._

import scala.concurrent.{ExecutionContext, Future}

trait Network extends LazyLogging {
  val passphrase: String
  lazy val networkId: Array[Byte] = sha256(passphrase.getBytes(UTF_8))
  val horizon: HorizonAccess

  /**
    * The keypair for the master account for this network
    */
  lazy val masterAccount: KeyPair =
    KeyPair.fromSecretSeed(ByteArrays.sha256(passphrase.getBytes("UTF-8")))

  /**
    * Submit the SignedTransaction to the network and eventually receive a TransactionPostResp with the results.
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-create.html endpoint doc]]
    */
  def submit(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResp] = horizon.post(txn)

  /**
    * Fetch details regarding the account identified by `pubKey`.
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/accounts-single.html endpoint doc]]
    */
  def account(pubKey: PublicKeyOps)(implicit ec: ExecutionContext): Future[AccountResp] =
    horizon.get[AccountResp](s"/accounts/${pubKey.accountId}")

  /**
    * Fetch value for single data field associated with an account.
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/data-for-account.html endpoint doc]]
    */
  def accountData(pubKey: PublicKeyOps, dataKey: String)(implicit ec: ExecutionContext): Future[String] =
    horizon.get[DataValueResp](s"/accounts/${pubKey.accountId}/data/$dataKey").map(_.v).map(base64).map(new String(_))

  /**
    * Fetch a stream of assets, optionally filtered by code, issuer or neither
    * @param code optional code to filter by (defaults to `None`)
    * @param issuer optional issuer account to filter by (defaults to `None`)
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/assets-all.html endpoint doc]]
    */
  def assets(code: Option[String] = None, issuer: Option[PublicKeyOps] = None,
             cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext):
            Future[Stream[AssetResp]] = {
    val params = Seq(code.map("asset_code" -> _), issuer.map("asset_issuer" -> _.accountId)).flatten.toMap
    horizon.getStream[AssetResp](s"/assets", AssetRespDeserializer, cursor, order, params)
  }

  /**
    * Fetch a stream of effects.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-all.html endpoint doc]]
    */
  def effects(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext): Future[Stream[EffectResp]] =
    horizon.getStream[EffectResp]("/effects", EffectRespDeserializer, cursor, order)

  /**
    * Fetch a stream of effects for a given account.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-account.html endpoint doc]]
    */
  def effectsByAccount(account: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext):
                      Future[Stream[EffectResp]] =
    horizon.getStream[EffectResp](s"/accounts/${account.accountId}/effects", EffectRespDeserializer, cursor, order)

  /**
    * Fetch a stream of effects for a given ledger.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-ledger.html endpoint doc]]
    */
  def effectsByLedger(ledgerId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext):
                     Future[Stream[EffectResp]] =
    horizon.getStream[EffectResp](s"/ledgers/$ledgerId/effects", EffectRespDeserializer, cursor, order)

  /**
    * Fetch a stream of effects for a given transaction hash.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-transaction.html endpoint doc]]
    */
  def effectsByTransaction(txnHash: String, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext):
                          Future[Stream[EffectResp]] =
    horizon.getStream[EffectResp](s"/transactions/$txnHash/effects", EffectRespDeserializer, cursor, order)


  /** Fetch a stream of effects for a given operation.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-operation.html endpoint doc]]
    */
  def effectsByOperation(operationId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext):
                     Future[Stream[EffectResp]] =
    horizon.getStream[EffectResp](s"/operations/$operationId/effects", EffectRespDeserializer, cursor, order)

  /**
    * Fetch a stream of details about ledgers.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/ledgers-all.html endpoint doc]]
    */
  def ledgers(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext): Future[Stream[LedgerResp]] =
    horizon.getStream[LedgerResp]("/ledgers", LedgerRespDeserializer, cursor, order)

  /**
    * Fetch details of a ledger by its id
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/ledgers-single.html endpoint doc]]
    */
  def ledger(ledgerId: Long)(implicit ex: ExecutionContext): Future[LedgerResp] =
    horizon.get[LedgerResp](s"/ledgers/$ledgerId")

  /**
    * Fetch a stream of offers for an account.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/offers-for-account.html endpoint doc]]
    */
  def offersByAccount(account: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                     Future[Stream[OfferResp]] =
    horizon.getStream[OfferResp](s"/accounts/${account.accountId}/offers", OfferRespDeserializer, cursor, order)

  /**
    * Fetch operation details by its id
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-single.html endpoint doc]]
    */
  def operation(operationId: Long)(implicit ex: ExecutionContext): Future[Transacted[Operation]] =
    horizon.get[Transacted[Operation]](s"/operations/$operationId")

  /**
    * Fetch a stream of operations.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-all.html endpoint doc]]
    */
  def operations(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext): Future[Stream[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"/operations", TransactedOperationDeserializer, cursor, order)

  /**
    * A source of all operations from the cursor in ascending order, forever.
    * @param cursor optional record id to start results from (defaults to `Now`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-all.html endpoint doc]]
    */
  def operationsSource(cursor: HorizonCursor = Now)(implicit ex: ExecutionContext): Source[Transacted[Operation], NotUsed] = {
    horizon.getSource("/operations", TransactedOperationDeserializer, cursor)
  }

  /**
    * Fetch a stream of operations, filtered by account.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-account.html endpoint doc]]
    */
  def operationsByAccount(pubKey: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                          Future[Stream[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"/accounts/${pubKey.accountId}/operations", TransactedOperationDeserializer, cursor, order)

  /**
    * A source of all operations filtered by account from the cursor in ascending order, forever.
    * @param cursor optional record id to start results from (defaults to `Now`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-account.html endpoint doc]]
    */
  def operationsByAccountSource(pubKey: PublicKeyOps, cursor: HorizonCursor = Now)
                               (implicit ex: ExecutionContext): Source[Transacted[Operation], NotUsed] = {
    horizon.getSource[Transacted[Operation]](s"/accounts/${pubKey.accountId}/operations", TransactedOperationDeserializer, cursor)
  }

  /**
    * Fetch a stream of operations, filtered by ledger id.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-ledger.html endpoint doc]]
    */
  def operationsByLedger(ledgerId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                        Future[Stream[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"/ledgers/$ledgerId/operations", TransactedOperationDeserializer, cursor, order)

  /**
    * Fetch a stream of operations, filtered by transaction hash.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-transaction.html endpoint doc]]
    */
  def operationsByTransaction(txnHash: String, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                             Future[Stream[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"/transactions/$txnHash/operations", TransactedOperationDeserializer, cursor, order)

  /**
    * Fetch details of the current orderbook for the given asset pairs.
    * @param selling the asset being offered
    * @param buying the asset being sought
    * @param limit the maximun quantity of offers to return, should the order book depth exceed this value.
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/orderbook-details.html endpoint doc]]
    */
  def orderBook(selling: Asset, buying: Asset, limit: Int = 20)(implicit ex: ExecutionContext): Future[OrderBook] = {
    val params = assetParams("selling", selling) ++ assetParams("buying", buying).updated("limit", limit.toString)
    horizon.get[OrderBook]("/order_book", params)
  }

  /**
    * A source of orders in the orderbook for the given asset pairs.
    * @param selling the asset being offered
    * @param buying the asset being sought
    * @param cursor optional record id to start results from (defaults to `Now`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/orderbook-details.html endpoint doc]]
    */
  def orderBookSource(selling: Asset, buying: Asset, cursor: HorizonCursor = Now)
                     (implicit ex: ExecutionContext): Source[OrderBook, NotUsed] = {
    val params = assetParams("selling", selling) ++ assetParams("buying", buying)
    horizon.getSource("/order_book", OrderBookDeserializer, cursor, params)
  }

  /**
    * Fetch a stream of payment operations.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/payments-all.html endpoint doc]]
    */
  def payments(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
              Future[Stream[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]]("/payments", TransactedOperationDeserializer, cursor, order)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * A source of all payment operations from the cursor in ascending order, forever.
    * @param cursor optional record id to start results from (defaults to `Now`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/payments-all.html endpoint doc]]
    */
  def paymentsSource(cursor: HorizonCursor = Now)(implicit ex: ExecutionContext): Source[Transacted[PayOperation], NotUsed] = {
    horizon.getSource("/payments", TransactedOperationDeserializer, cursor)
      .map(_.asInstanceOf[Transacted[PayOperation]])
  }

  /**
    * Fetch a stream of payment operations filtered by account.
    * @param pubKey the relevant account
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/payments-for-account.html endpoint doc]]
    */
  def paymentsByAccount(pubKey: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)
                       (implicit ex: ExecutionContext): Future[Stream[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"/accounts/${pubKey.accountId}/payments", TransactedOperationDeserializer, cursor, order)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * A source of all payment operations filtered by account from the cursor in ascending order, forever.
    * @param pubKey the relevant account
    * @param cursor optional record id to start results from (defaults to `Now`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/payments-all.html endpoint doc]]
    */
  def paymentsByAccountSource(pubKey: PublicKeyOps, cursor: HorizonCursor = Now)
                             (implicit ex: ExecutionContext): Source[Transacted[PayOperation], NotUsed] = {
    horizon.getSource[Transacted[Operation]](s"/accounts/${pubKey.accountId}/payments", TransactedOperationDeserializer, cursor)
      .map(_.asInstanceOf[Transacted[PayOperation]])
  }

  /**
    * Fetch a stream of payment operations filtered by ledger id.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/payments-for-ledger.html endpoint doc]]
    */
  def paymentsByLedger(ledgerId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                      Future[Stream[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"/ledgers/$ledgerId/payments", TransactedOperationDeserializer, cursor, order)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of payment operations filtered by transaction hash.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-transaction.html endpoint doc]]
    */
  def paymentsByTransaction(txnHash: String, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                           Future[Stream[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"/transactions/$txnHash/payments", TransactedOperationDeserializer, cursor, order)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of trades
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/trades.html endpoint doc]]
    */
  def trades(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext): Future[Stream[Trade]] =
    horizon.getStream[Trade]("/trades", TradeDeserializer, cursor, order)

  /**
    * Fetch a stream of trades filtered by orderbook
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/trades.html endpoint doc]]
    */
  def tradesByOrderBook(base: Asset, counter: Asset, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                       Future[Stream[Trade]] = {
    val params = assetParams("base", base) ++ assetParams("counter", counter)
    horizon.getStream[Trade]("/trades", TradeDeserializer, cursor, order, params)
  }

  /**
    * Fetch a stream of trades filtered by offer id
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/trades.html endpoint doc]]
    */
  def tradesByOfferId(offerId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                     Future[Stream[Trade]] = {
    val params = Map("offerid" -> s"$offerId")
    horizon.getStream[Trade]("/trades", TradeDeserializer, cursor, order, params)
  }

  /**
    * Fetch a stream of historical transactions from the cursor.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-all.html endpoint doc]]
    */
  def transactions(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext): Future[Stream[TransactionHistoryResp]] = {
    horizon.getStream[TransactionHistoryResp]("/transactions", TransactionHistoryRespDeserializer, cursor, order)
  }

  /**
    * A source of all transactions from the cursor in ascending order, forever.
    * @param cursor optional record id to start results from (defaults to `Now`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-all.html endpoint doc]]
    */
  def transactionSource(cursor: HorizonCursor = Now)(implicit ex: ExecutionContext): Source[TransactionHistoryResp, NotUsed] = {
    horizon.getSource[TransactionHistoryResp]("/transactions", TransactionHistoryRespDeserializer, cursor)
  }

  /**
    * Fetch a stream of transactions affecting a given account
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-for-account.html endpoint doc]]
    */
  def transactionsByAccount(pubKey: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                           Future[Stream[TransactionHistoryResp]] = {
    horizon.getStream[TransactionHistoryResp](s"/accounts/${pubKey.accountId}/transactions", TransactionHistoryRespDeserializer, cursor, order)
  }

  /**
    * A source of all transactions affecting a given account from the cursor in ascending order, forever.
    * @param cursor optional record id to start results from (defaults to `Now`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-for-account.html endpoint doc]]
    */
  def transactionsByAccountSource(pubKey: PublicKeyOps, cursor: HorizonCursor = Now)(implicit ex: ExecutionContext): Source[TransactionHistoryResp, NotUsed] = {
    horizon.getSource[TransactionHistoryResp](s"/accounts/${pubKey.accountId}/transactions", TransactionHistoryRespDeserializer, cursor)
  }

  /**
    * Fetch a stream of transactions for a given ledger
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-for-ledger.html endpoint doc]]
    */
  def transactionsByLedger(sequenceId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                          Future[Stream[TransactionHistoryResp]] = {
    horizon.getStream[TransactionHistoryResp](s"/ledgers/$sequenceId/transactions", TransactionHistoryRespDeserializer, cursor, order)
  }

  /**
    * A source of all transactions for a given ledger from the cursor in ascending order, forever.
    * @param cursor optional record id to start results from (defaults to `Now`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-for-ledger.html endpoint doc]]
    */
  def transactionsByLedgerSource(sequenceId: Long, cursor: HorizonCursor = Now)(implicit ex: ExecutionContext): Source[TransactionHistoryResp, NotUsed] = {
    horizon.getSource[TransactionHistoryResp](s"/ledgers/$sequenceId/transactions", TransactionHistoryRespDeserializer, cursor)
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

/**
  * A feature on certain networks (notably TestNet) for funding new accounts.
  */
trait FriendBot {
  val horizon: HorizonAccess
  def fund(pk: PublicKeyOps)(implicit ec: ExecutionContext): Future[TransactionPostResp] =
    horizon.get[TransactionPostResp]("/friendbot", Map("addr" -> pk.accountId))
}

case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
  val horizon = new Horizon(URI.create("https://horizon.stellar.org"))
}

case object TestNetwork extends Network with FriendBot {
  override val passphrase = "Test SDF Network ; September 2015"
  val horizon = new Horizon(URI.create("https://horizon-testnet.stellar.org"))
}

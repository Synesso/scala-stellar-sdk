package stellar.sdk

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import stellar.sdk.inet._
import stellar.sdk.model._
import stellar.sdk.model.op.{Operation, PayOperation, Transacted, TransactedOperationDeserializer}
import stellar.sdk.model.response._
import stellar.sdk.model.result._
import stellar.sdk.util.ByteArrays._

import scala.concurrent.{ExecutionContext, Future}

trait Network extends LazyLogging {
  def passphrase: String
  lazy val networkId: Array[Byte] = sha256(passphrase.getBytes(UTF_8))
  val horizon: HorizonAccess

  /**
    * The keypair for the master account for this network
    */
  lazy val masterAccount: KeyPair = KeyPair.fromSecretSeed(networkId)

  /**
    * Fetches information on the Horizon and Core version underlying this network.
    */
  def info()(implicit ec: ExecutionContext): Future[NetworkInfo] = horizon.get[NetworkInfo]("/")

  /**
    * Submit the SignedTransaction to the network and eventually receive a TransactionPostResp with the results.
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-create.html endpoint doc]]
    */
  def submit(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResponse] = {
    val payeeAccounts =
      if (txn.hasMemo || txn.transaction.overrideMemoRequirement) Future(List())
      else Future.sequence((txn.payeeAccounts.toSet -- txn.createdAccounts.toSet).map(_.publicKey).map(account))
    for {
      accountsRequiringMemo <- payeeAccounts.map(_.filter(_.isMemoRequired))
      response <-
        if (accountsRequiringMemo.nonEmpty) Future.failed(InvalidTransactionException(
          s"No memo provided, but required by ${accountsRequiringMemo.map(_.id.accountId).mkString(",")}"))
        else horizon.post(txn)
    } yield response
  }

  /**
    * Fetch details regarding the account identified by `pubKey`.
    * @param pubKey the relevant account
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/accounts-single.html endpoint doc]]
    */
  def account(pubKey: PublicKeyOps)(implicit ec: ExecutionContext): Future[AccountResponse] =
    horizon.get[AccountResponse](s"accounts/${pubKey.accountId}")

  /**
    * Fetch value for single data field associated with an account.
    * @param pubKey the relevant account
    * @param dataKey the key for the data field
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/data-for-account.html endpoint doc]]
    */
  def accountData(pubKey: PublicKeyOps, dataKey: String)(implicit ec: ExecutionContext): Future[Array[Byte]] = {
    val encodedKey = URLEncoder.encode(dataKey, "UTF-8")
    horizon.get[DataValueResponse](s"accounts/${pubKey.accountId}/data/$encodedKey")
      .map(_.v).map(base64)
  }

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
            Future[LazyList[AssetResponse]] = {
    val params = Seq(code.map("asset_code" -> _), issuer.map("asset_issuer" -> _.accountId)).flatten.toMap
    horizon.getStream[AssetResponse](s"assets", AssetRespDeserializer, cursor, order, params)
  }

  /**
    * Fetch a stream of effects.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-all.html endpoint doc]]
    */
  def effects(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext): Future[LazyList[EffectResponse]] =
    horizon.getStream[EffectResponse]("effects", EffectResponseDeserializer, cursor, order)

  /**
    * Fetch a stream of effects for a given account.
    * @param account the relevant account
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-account.html endpoint doc]]
    */
  def effectsByAccount(account: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext):
                      Future[LazyList[EffectResponse]] =
    horizon.getStream[EffectResponse](s"accounts/${account.accountId}/effects", EffectResponseDeserializer, cursor, order)

  /**
    * Fetch a stream of effects for a given ledger.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-ledger.html endpoint doc]]
    */
  def effectsByLedger(ledgerId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext):
                     Future[LazyList[EffectResponse]] =
    horizon.getStream[EffectResponse](s"ledgers/$ledgerId/effects", EffectResponseDeserializer, cursor, order)

  /**
    * Fetch a stream of effects for a given transaction hash.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-transaction.html endpoint doc]]
    */
  def effectsByTransaction(txnHash: String, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext):
                          Future[LazyList[EffectResponse]] =
    horizon.getStream[EffectResponse](s"transactions/$txnHash/effects", EffectResponseDeserializer, cursor, order)


  /** Fetch a stream of effects for a given operation.
    * param cursor optional record id to start results from (defaults to `0`)
    * param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/effects-for-operation.html endpoint doc]]
    */
  def effectsByOperation(operationId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext):
                     Future[LazyList[EffectResponse]] =
    horizon.getStream[EffectResponse](s"operations/$operationId/effects", EffectResponseDeserializer, cursor, order)

  /**
    * Fetch a stream of details about ledgers.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/ledgers-all.html endpoint doc]]
    */
  def ledgers(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ec: ExecutionContext): Future[LazyList[LedgerResponse]] =
    horizon.getStream[LedgerResponse]("ledgers", LedgerRespDeserializer, cursor, order)

  /**
    * Fetch details of a ledger by its id
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/ledgers-single.html endpoint doc]]
    */
  def ledger(ledgerId: Long)(implicit ex: ExecutionContext): Future[LedgerResponse] =
    horizon.get[LedgerResponse](s"ledgers/$ledgerId")

  /**
   * Fetch a stream of offers.
   * @param sponsor optional account of the sponsor who is paying the reserves for the offer
   * @param account optional account making the offer
   * @param selling optional asset being sold
   * @param buying optional asset being bought
   * @param cursor optional record id to start results from (defaults to `0`)
   * @param order  optional order to sort results by (defaults to `Asc`)
   * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/offers.html endpoint doc]]
   */
  def offers(sponsor: Option[PublicKeyOps] = None, account: Option[PublicKeyOps] = None, selling: Option[Asset] = None,
             buying: Option[Asset] = None, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
  Future[LazyList[OfferResponse]] = {
    val params =
      optional("sponsor", sponsor.map(_.accountId)) ++
      optional("seller", account.map(_.accountId)) ++
      optional(buying.map(asset => assetParams("buying", asset))) ++
      optional(selling.map(asset => assetParams("selling", asset)))
    horizon.getStream[OfferResponse](s"offers", OfferRespDeserializer, cursor, order, params)
  }

  /**
    * Fetch a stream of offers for an account.
    * @param account the relevant account
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/offers-for-account.html endpoint doc]]
    */
  def offersByAccount(account: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                     Future[LazyList[OfferResponse]] =
    horizon.getStream[OfferResponse](s"accounts/${account.accountId}/offers", OfferRespDeserializer, cursor, order)

  /**
    * Fetch operation details by its id
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-single.html endpoint doc]]
    */
  def operation(operationId: Long)(implicit ex: ExecutionContext): Future[Transacted[Operation]] =
    horizon.get[Transacted[Operation]](s"operations/$operationId")

  /**
    * Fetch a stream of operations.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-all.html endpoint doc]]
    */
  def operations(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext): Future[LazyList[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"operations", TransactedOperationDeserializer, cursor, order)

  /**
    * Fetch a stream of operations, filtered by account.
    * @param pubKey the relevant account
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-account.html endpoint doc]]
    */
  def operationsByAccount(pubKey: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                          Future[LazyList[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"accounts/${pubKey.accountId}/operations", TransactedOperationDeserializer, cursor, order)

  /**
   * Fetch a stream of operations, filtered by claimable balance id.
   * @param cursor optional record id to start results from (defaults to `0`)
   * @param order  optional order to sort results by (defaults to `Asc`)
   * @see [[https://developers.stellar.org/api/resources/claimablebalances/operations/ endpoint doc]]
   */
  def operationsByClaim(
    id: ClaimableBalanceId,
    cursor: HorizonCursor = Record(0),
    order: HorizonOrder = Asc
  )(implicit ex: ExecutionContext): Future[LazyList[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"claimable_balances/${id.encodeString}/operations",
      TransactedOperationDeserializer, cursor, order)

  /**
    * Fetch a stream of operations, filtered by ledger id.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-ledger.html endpoint doc]]
    */
  def operationsByLedger(ledgerId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                        Future[LazyList[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"ledgers/$ledgerId/operations", TransactedOperationDeserializer, cursor, order)

  /**
    * Fetch a stream of operations, filtered by transaction hash.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-transaction.html endpoint doc]]
    */
  def operationsByTransaction(txnHash: String, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                             Future[LazyList[Transacted[Operation]]] =
    horizon.getStream[Transacted[Operation]](s"transactions/$txnHash/operations", TransactedOperationDeserializer, cursor, order)

  /**
    * Fetch details of the current orderbook for the given asset pairs.
    * @param selling the asset being offered
    * @param buying the asset being sought
    * @param limit the maximum quantity of offers to return, should the order book depth exceed this value.
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/orderbook-details.html endpoint doc]]
    */
  def orderBook(selling: Asset, buying: Asset, limit: Int = 20)(implicit ex: ExecutionContext): Future[OrderBook] = {
    val params = assetParams("selling", selling) ++ assetParams("buying", buying).updated("limit", limit.toString)
    horizon.get[OrderBook]("order_book", params)
  }

  /**
    * Fetch a stream of payment operations.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/payments-all.html endpoint doc]]
    */
  def payments(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
              Future[LazyList[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]]("payments", TransactedOperationDeserializer, cursor, order)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of payment operations filtered by account.
    * @param pubKey the relevant account
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/payments-for-account.html endpoint doc]]
    */
  def paymentsByAccount(pubKey: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)
                       (implicit ex: ExecutionContext): Future[LazyList[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"accounts/${pubKey.accountId}/payments", TransactedOperationDeserializer, cursor, order)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of payment operations filtered by ledger id.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/payments-for-ledger.html endpoint doc]]
    */
  def paymentsByLedger(ledgerId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                      Future[LazyList[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"ledgers/$ledgerId/payments", TransactedOperationDeserializer, cursor, order)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of payment operations filtered by transaction hash.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/operations-for-transaction.html endpoint doc]]
    */
  def paymentsByTransaction(txnHash: String, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                           Future[LazyList[Transacted[PayOperation]]] =
    horizon.getStream[Transacted[Operation]](s"transactions/$txnHash/payments", TransactedOperationDeserializer, cursor, order)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  /**
    * Fetch a stream of trades
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/trades.html endpoint doc]]
    */
  def trades(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext): Future[LazyList[Trade]] =
    horizon.getStream[Trade]("trades", TradeDeserializer, cursor, order)

  /**
    * Fetch a stream of trades filtered by orderbook
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/trades.html endpoint doc]]
    */
  def tradesByOrderBook(base: Asset, counter: Asset, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                       Future[LazyList[Trade]] = {
    val params = assetParams("base", base) ++ assetParams("counter", counter)
    horizon.getStream[Trade]("trades", TradeDeserializer, cursor, order, params)
  }

  /**
    * Fetch a stream of trades filtered by offer id
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/trades.html endpoint doc]]
    */
  def tradesByOfferId(offerId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                     Future[LazyList[Trade]] = {
    val params = Map("offerid" -> s"$offerId")
    horizon.getStream[Trade]("trades", TradeDeserializer, cursor, order, params)
  }

  /**
    * Fetch trade history aggregations in a time period, for a given resolution and asset pair.
    * @param start the start of the time period
    * @param end the end of the time period
    * @param resolution the resolution for reporting trading activity
    * @param offsetHours an offset to apply to the start of aggregated periods, between 0-23.
    * @param base the base asset
    * @param counter the counter/quote asset
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/trade_aggregations.html endpoint doc]]
    */
  def tradeAggregations(start: Instant, end: Instant, resolution: TradeAggregation.Resolution, offsetHours: Int,
                        base: Asset, counter: Asset, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)
                       (implicit ex: ExecutionContext): Future[LazyList[TradeAggregation]] = {

    val params = assetParams("base", base) ++ assetParams("counter", counter) ++ Map(
      "start_time" -> start.toEpochMilli.toString,
      "end_time" -> end.toEpochMilli.toString,
      "resolution" -> resolution.duration.toMillis.toString,
      "offset" -> TimeUnit.HOURS.toMillis(offsetHours).toString
    )
    horizon.getStream[TradeAggregation]("trade_aggregations", TradeAggregationDeserializer, cursor, order, params)
  }

  /**
    * Fetch information on a single transaction.
    * @param transactionId transaction to fetch
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-single.html endpoint doc]]
    */
  def transaction(transactionId: String)(implicit ex: ExecutionContext): Future[TransactionHistory] = {
    horizon.get[TransactionHistory](s"transactions/$transactionId")
  }

  /**
    * Fetch a stream of historical transactions from the cursor.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-all.html endpoint doc]]
    */
  def transactions(cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext): Future[LazyList[TransactionHistory]] = {
    horizon.getStream[TransactionHistory]("transactions", TransactionHistoryDeserializer, cursor, order)
  }

  /**
    * Fetch a stream of transactions affecting a given account.
    * @param pubKey the relevant account
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-for-account.html endpoint doc]]
    */
  def transactionsByAccount(pubKey: PublicKeyOps, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                           Future[LazyList[TransactionHistory]] = {
    horizon.getStream[TransactionHistory](s"accounts/${pubKey.accountId}/transactions", TransactionHistoryDeserializer, cursor, order)
  }

  /**
    * Fetch a stream of transactions for a given ledger.
    * @param cursor optional record id to start results from (defaults to `0`)
    * @param order  optional order to sort results by (defaults to `Asc`)
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/transactions-for-ledger.html endpoint doc]]
    */
  def transactionsByLedger(sequenceId: Long, cursor: HorizonCursor = Record(0), order: HorizonOrder = Asc)(implicit ex: ExecutionContext):
                          Future[LazyList[TransactionHistory]] = {
    horizon.getStream[TransactionHistory](s"ledgers/$sequenceId/transactions", TransactionHistoryDeserializer, cursor, order)
  }

  /**
    * Returns the fee statistics for the last ledger.
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/fee-stats.html endpoint doc]]
    */
  def feeStats()(implicit ex: ExecutionContext): Future[FeeStatsResponse] =
    horizon.get[FeeStatsResponse]("fee_stats")

  /**
    * Fetch a stream of payment paths that realise a payment of the requested destination amount, from the specified
    * account.
    * @param from the account that wishes to make the payment
    * @param to the recipient account
    * @param amount the desired payment amount
    * @see [[https://www.stellar.org/developers/horizon/reference/endpoints/path-finding.html endpoint doc]]
    */
  def paths(from: PublicKeyOps, to: PublicKeyOps, amount: Amount)(implicit ex: ExecutionContext): Future[Seq[PaymentPath]] = {
    val queryParams = Map(
      "source_account" -> from.accountId,
      "destination_account" -> to.accountId
    ) ++ amountParams("destination", amount)
    horizon.getSeq[PaymentPath]("paths", PaymentPathDeserializer, queryParams)
  }

  /**
   * Fetch a single claimable balance by its id.
   */
  def claim(id: ClaimableBalanceId)(implicit ex: ExecutionContext): Future[ClaimableBalance] =
    horizon.get[ClaimableBalance](s"claimable_balances/${id.encodeString}")

  /**
   * Fetch a list of claimable balances for a claimant account.
   *
   * @param claimant The account that can claim the balances.
   */
  def claimsByClaimant(claimant: PublicKeyOps)(implicit ex: ExecutionContext): Future[Seq[ClaimableBalance]] = {
    val queryParams = Map("claimant" -> claimant.accountId)
    horizon.getSeq[ClaimableBalance]("claimable_balances", ClaimableBalanceDeserializer, queryParams)
  }

  /**
   * Fetch a list of claimable balances for an assest.
   *
   * @param asset The asset to be claimed.
   */
  def claimsByAsset(asset: Asset)(implicit ex: ExecutionContext): Future[Seq[ClaimableBalance]] = {
    val queryParams = Map("asset" -> asset.canoncialString)
    horizon.getSeq[ClaimableBalance]("claimable_balances", ClaimableBalanceDeserializer, queryParams)
  }

  /**
   * Fetch a list of claimable balances for a sponsoring account.
   *
   * @param sponsor the account that is sponsoring this balance.
   */
  def claimsBySponsor(sponsor: PublicKeyOps)(implicit ex: ExecutionContext): Future[Seq[ClaimableBalance]] = {
    val queryParams = Map("sponsor" -> sponsor.accountId)
    horizon.getSeq[ClaimableBalance]("claimable_balances", ClaimableBalanceDeserializer, queryParams)
  }

  private def amountParams(prefix: String, amount: Amount): Map[String, String] = {
    (amount match {
      case issuedAmount: IssuedAmount => assetParams(prefix, issuedAmount.asset)
      case _ => Map.empty[String, String]
    }) ++ Map(s"${prefix}_amount" -> s"${amount.units}")
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

  private def optional(params: Option[Map[String, String]]): Map[String, String] = params.getOrElse(Map.empty[String,String])

  private def optional(key: String, value: Option[String]): Map[String, String] = value.fold(Map.empty[String,String])(v => Map(key -> v))
}

/**
  * A feature on TestNetwork (and optionally on StandaloneNetworks) for funding new accounts.
  */
trait FriendBot {
  val horizon: HorizonAccess
  def fund(pk: PublicKeyOps)(implicit ec: ExecutionContext): Future[TransactionPostResponse] =
    horizon.get[TransactionPostResponse]("friendbot", Map("addr" -> pk.accountId))
}


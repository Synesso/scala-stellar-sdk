package stellar.sdk

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import stellar.sdk.inet.Server
import stellar.sdk.op.{Operation, PayOperation, Transacted, TransactedOperationDeserializer}
import stellar.sdk.resp._

import scala.concurrent.{ExecutionContext, Future}

trait Network extends ByteArrays {
  val passphrase: String
  lazy val networkId: Array[Byte] = sha256(passphrase.getBytes(UTF_8)).get
  val server: Server

  def submit(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionResp] = server.post(txn)

  def account(pubKey: PublicKeyOps)(implicit ec: ExecutionContext): Future[AccountResp] =
    server.get[AccountResp](s"/accounts/${pubKey.accountId}")

  def accountData(pubKey: PublicKeyOps, dataKey: String)(implicit ec: ExecutionContext): Future[String] =
    server.get[DataValueResp](s"/accounts/${pubKey.accountId}/data/$dataKey").map(_.v).map(base64).map(new String(_))

  def assets(code: Option[String] = None, issuer: Option[String] = None)(implicit ec: ExecutionContext): Future[Stream[AssetResp]] = {
    val params = Seq(code.map("asset_code" -> _), issuer.map("asset_issuer" -> _)).flatten.toMap
    server.getStream[AssetResp](s"/assets", AssetRespDeserializer, params)
  }

  def effects()(implicit ec: ExecutionContext): Future[Stream[EffectResp]] =
    server.getStream[EffectResp]("/effects", EffectRespDeserializer)

  def effectsByAccount(account: PublicKeyOps)(implicit ec: ExecutionContext): Future[Stream[EffectResp]] =
    server.getStream[EffectResp](s"/accounts/${account.accountId}/effects", EffectRespDeserializer)

  def effectsByLedger(sequenceId: Long)(implicit ec: ExecutionContext): Future[Stream[EffectResp]] =
    server.getStream[EffectResp](s"/ledgers/$sequenceId/effects", EffectRespDeserializer)

  def ledgers()(implicit ec: ExecutionContext): Future[Stream[LedgerResp]] =
    server.getStream[LedgerResp](s"/ledgers", LedgerRespDeserializer)

  def ledger(sequenceId: Long)(implicit ex: ExecutionContext): Future[LedgerResp] =
    server.get[LedgerResp](s"/ledgers/$sequenceId")

  def offersByAccount(pubKey: PublicKeyOps)(implicit ex: ExecutionContext): Future[Stream[OfferResp]] =
    server.getStream[OfferResp](s"/accounts/${pubKey.accountId}/offers", OfferRespDeserializer)

  def operation(operationId: Long)(implicit ex: ExecutionContext): Future[Transacted[Operation]] =
    server.get[Transacted[Operation]](s"/operations/$operationId")

  def operations()(implicit ex: ExecutionContext): Future[Stream[Transacted[Operation]]] =
    server.getStream[Transacted[Operation]](s"/operations", TransactedOperationDeserializer)

  def operationsByAccount(pubKey: PublicKeyOps)(implicit ex: ExecutionContext): Future[Stream[Transacted[Operation]]] =
    server.getStream[Transacted[Operation]](s"/accounts/${pubKey.accountId}/operations", TransactedOperationDeserializer)

  def operationsByLedger(ledgerId: Long)(implicit ex: ExecutionContext): Future[Stream[Transacted[Operation]]] =
    server.getStream[Transacted[Operation]](s"/ledgers/$ledgerId/operations", TransactedOperationDeserializer)

  def operationsByTransaction(txnHash: String)(implicit ex: ExecutionContext): Future[Stream[Transacted[Operation]]] =
    server.getStream[Transacted[Operation]](s"/transactions/$txnHash/operations", TransactedOperationDeserializer)

  def orderBook(selling: Asset, buying: Asset, limit: Int = 20)(implicit ex: ExecutionContext): Future[OrderBook] = {
    def assetParams(prefix: String, asset: Asset): Map[String, Any] = {
      asset match {
        case NativeAsset => Map(s"${prefix}_asset_type" -> "native")
        case nna: NonNativeAsset => Map(
          s"${prefix}_asset_type" -> nna.typeString,
          s"${prefix}_asset_code" -> nna.code,
          s"${prefix}_asset_issuer" -> nna.issuer.accountId
        )
      }
    }

    val params = assetParams("selling", selling) ++ assetParams("buying", buying).updated("limit", limit)
    server.get[OrderBook]("/order_book", params)
  }

  def payments()(implicit ex: ExecutionContext): Future[Stream[Transacted[PayOperation]]] =
    server.getStream[Transacted[Operation]](s"/payments", TransactedOperationDeserializer)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  def paymentsByAccount(pubKey: PublicKeyOps)(implicit ex: ExecutionContext): Future[Stream[Transacted[PayOperation]]] =
    server.getStream[Transacted[Operation]](s"/accounts/${pubKey.accountId}/payments", TransactedOperationDeserializer)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  def paymentsByLedger(ledgerId: Long)(implicit ex: ExecutionContext): Future[Stream[Transacted[PayOperation]]] =
    server.getStream[Transacted[Operation]](s"/ledgers/$ledgerId/payments", TransactedOperationDeserializer)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

  def paymentsByTransaction(txnHash: String)(implicit ex: ExecutionContext): Future[Stream[Transacted[PayOperation]]] =
    server.getStream[Transacted[Operation]](s"/transactions/$txnHash/payments", TransactedOperationDeserializer)
      .map(_.map(_.asInstanceOf[Transacted[PayOperation]]))

}

case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
  override val server = Server(URI.create("https://horizon.stellar.org"))
}

case object TestNetwork extends Network {
  override val passphrase = "Test SDF Network ; September 2015"
  override val server = Server(URI.create("https://horizon-testnet.stellar.org"))
  implicit val backend = AkkaHttpBackend()

  def fund(pk: PublicKeyOps)(implicit ec: ExecutionContext): Future[TransactionResp] =
    server.get[TransactionResp]("friendbot", Map("addr" -> pk.accountId))
}

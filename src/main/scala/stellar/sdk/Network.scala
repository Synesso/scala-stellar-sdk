package stellar.sdk

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import stellar.sdk.inet.Server
import stellar.sdk.resp._

import scala.concurrent.{ExecutionContext, Future}

trait Network extends ByteArrays {
  val passphrase: String
  lazy val networkId: Array[Byte] = sha256(passphrase.getBytes(UTF_8)).get
  val server: Server
  def submit(txn: SignedTransaction): Future[SubmitTransactionResponse] = server.post(txn)

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

}

case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
  override val server = Server(URI.create("https://horizon.stellar.org"))
}

case object TestNetwork extends Network {
  override val passphrase = "Test SDF Network ; September 2015"
  override val server = Server(URI.create("https://horizon-testnet.stellar.org"))
  implicit val backend = AkkaHttpBackend()

  def fund(pk: PublicKeyOps)(implicit ec: ExecutionContext): Future[FundTestAccountResp] =
    server.get[FundTestAccountResp]("friendbot", Map("addr" -> pk.accountId))
}

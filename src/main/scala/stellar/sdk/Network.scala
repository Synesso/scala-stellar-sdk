package stellar.sdk

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import stellar.sdk.inet.Server
import stellar.sdk.resp.{AccountResp, FundTestAccountResponse, SubmitTransactionResponse}

import scala.concurrent.{ExecutionContext, Future}

trait Network extends ByteArrays {
  val passphrase: String
  lazy val networkId: Array[Byte] = sha256(passphrase.getBytes(UTF_8)).get
  val server: Server
  def submit(txn: SignedTransaction): Future[SubmitTransactionResponse] = server.post(txn)

  def account(pubKey: PublicKeyOps)(implicit ec: ExecutionContext): Future[AccountResp] =
    server.get[AccountResp](s"/accounts/${pubKey.accountId}")

  def assets()(implicit ec: ExecutionContext): Future[Stream[Asset]] = server.getPages[Asset](s"/assets")

}

case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
  override val server = Server(URI.create("https://horizon.stellar.org"))
}

case object TestNetwork extends Network {
  override val passphrase = "Test SDF Network ; September 2015"
  override val server = Server(URI.create("https://horizon-testnet.stellar.org"))
  implicit val backend = AkkaHttpBackend()

  def fund(pk: PublicKeyOps)(implicit ec: ExecutionContext): Future[FundTestAccountResponse] =
    server.get[FundTestAccountResponse]("friendbot", Map("addr" -> pk.accountId))
}

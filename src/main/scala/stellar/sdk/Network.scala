package stellar.sdk

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

import stellar.sdk.inet.Server
import stellar.sdk.resp.{FundTestAccountResponse, SubmitTransactionResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http.Http

trait Network extends ByteArrays {
  val passphrase: String
  lazy val networkId: Array[Byte] = sha256(passphrase.getBytes(UTF_8)).get
  val server: Server
  def submit(txn: SignedTransaction): Future[SubmitTransactionResponse] = server.post(txn)
}

case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
  override val server = Server(URI.create("https://horizon.stellar.org"))
}

case object TestNetwork extends Network {
  override val passphrase = "Test SDF Network ; September 2015"
  override val server = Server(URI.create("https://horizon-testnet.stellar.org"))

  def fund(pk: PublicKeyOps)(): Future[FundTestAccountResponse] = Future {
    val response = Http(s"${server.uri.toString}/friendbot")
      .timeout(connTimeoutMs = 1000, readTimeoutMs = 30000)
      .param("addr", pk.accountId).asString
    assert(response.code == 200, s"HTTP Response code ${response.code}")
    FundTestAccountResponse(response.body).get
  }
}

package stellar.scala.sdk.inet

import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

import stellar.scala.sdk.resp.SubmitTransactionResponse
import stellar.scala.sdk.{ByteArrays, SignedTransaction}

import scala.concurrent.Future

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
}

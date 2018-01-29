package stellar.scala.sdk.net

import java.nio.charset.StandardCharsets.UTF_8

import stellar.scala.sdk.ByteArrays

trait Network extends ByteArrays {
  val passphrase: String
  lazy val networkId: Array[Byte] = sha256(passphrase.getBytes(UTF_8)).get
  def server: Server
}

case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
  override def server = Server("https://horizon.stellar.org")
}

case object TestNetwork extends Network {
  override val passphrase = "Test SDF Network ; September 2015"
  override def server = Server("https://horizon-testnet.stellar.org")
}

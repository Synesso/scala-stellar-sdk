package stellar.scala.sdk

import java.nio.charset.StandardCharsets.UTF_8

trait Network extends ByteArrays {
  val passphrase: String
  lazy val networkId: Array[Byte] = sha256(passphrase.getBytes(UTF_8)).get
}

case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
}

case object TestNetwork extends Network {
  override val passphrase = "Test SDF Network ; September 2015"
}

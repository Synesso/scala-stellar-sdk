package stellar.sdk

import java.net.URI

import stellar.sdk.inet.Horizon

/**
  * The public Stellar production network.
  */
case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
  val horizon = Horizon(URI.create("https://horizon.stellar.org"))
}



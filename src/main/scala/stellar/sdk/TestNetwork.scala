package stellar.sdk

import java.net.URI

import stellar.sdk.inet.Horizon

/**
  * The public Stellar test network.
  */
case object TestNetwork extends Network with FriendBot {
  override val passphrase = "Test SDF Network ; September 2015"
  val horizon = new Horizon(URI.create("https://horizon-testnet.stellar.org"))
}



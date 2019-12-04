package stellar.sdk

import okhttp3.HttpUrl
import stellar.sdk.inet.{HorizonAccess, OkHorizon}

/**
  * The public Stellar test network.
  */
case object TestNetwork extends Network with FriendBot {
  override val passphrase = "Test SDF Network ; September 2015"
  override val horizon: HorizonAccess = new OkHorizon(
    HttpUrl.parse("https://horizon-testnet.stellar.org"))
}



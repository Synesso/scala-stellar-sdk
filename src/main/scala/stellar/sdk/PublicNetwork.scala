package stellar.sdk

import okhttp3.HttpUrl
import stellar.sdk.inet.{HorizonAccess, OkHorizon}

/**
  * The public Stellar production network.
  */
case object PublicNetwork extends Network {
  override val passphrase = "Public Global Stellar Network ; September 2015"
  override val horizon: HorizonAccess = new OkHorizon(HttpUrl.parse("https://horizon.stellar.org"))
}



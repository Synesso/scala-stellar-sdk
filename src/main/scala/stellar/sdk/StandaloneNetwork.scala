package stellar.sdk

import okhttp3.HttpUrl
import stellar.sdk.inet.{HorizonAccess, OkHorizon}

/**
  * A network that represents the stand-alone docker image for Horizon & core.
  *
  * @see [[https://github.com/stellar/docker-stellar-core-horizon]]
  */
case class StandaloneNetwork(base: HttpUrl) extends Network with FriendBot {
  override val passphrase: String = "Standalone Network ; February 2017"
  override val horizon: HorizonAccess = new OkHorizon(base)
}


/**
  * A network that represents the stand-alone docker image for Horizon & core, on the default docker port of 8000.
  *
  * @see [[https://github.com/stellar/docker-stellar-core-horizon]]
  */
object LocalStandaloneNetwork extends StandaloneNetwork(HttpUrl.parse("http://localhost:8000"))

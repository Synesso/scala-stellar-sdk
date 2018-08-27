package stellar.sdk

import java.net.URI

import stellar.sdk.inet.{Horizon, HorizonAccess}

/**
  * A network that represents the stand-alone docker image for Horizon & core.
 *
  * @see [[https://github.com/stellar/docker-stellar-core-horizon]]
  */
case class StandaloneNetwork(port: Int = 8000) extends Network with FriendBot {
  override val passphrase: String = "Standalone Network ; February 2017"
  override val horizon: HorizonAccess = new Horizon(URI.create(s"http://localhost:$port"))
}

/**
  * A network that represents the stand-alone docker image for Horizon & core, on the default docker port of 8000.
  *
  * @see [[https://github.com/stellar/docker-stellar-core-horizon]]
  */
object StandaloneNetwork extends StandaloneNetwork(8000)

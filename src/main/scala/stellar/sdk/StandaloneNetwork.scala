package stellar.sdk

import java.net.URI

import stellar.sdk.inet.{Horizon, HorizonAccess}

/**
  * A network that represents the stand-alone docker image for Horizon & core.
  *
  * @see [[https://github.com/stellar/docker-stellar-core-horizon]]
  */
case class StandaloneNetwork(uri: URI) extends Network with FriendBot {
  override val passphrase: String = "Standalone Network ; February 2017"
  override val horizon: HorizonAccess = Horizon(uri)
}


/**
  * A network that represents the stand-alone docker image for Horizon & core, on the default docker port of 8000.
  *
  * @see [[https://github.com/stellar/docker-stellar-core-horizon]]
  */
object LocalStandaloneNetwork extends StandaloneNetwork(URI.create(s"http://localhost:8000"))

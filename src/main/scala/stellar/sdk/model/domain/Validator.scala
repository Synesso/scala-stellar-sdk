package stellar.sdk.model.domain

import okhttp3.HttpUrl
import stellar.sdk.PublicKey
import toml.Value
import toml.Value.Tbl

/**
  * Validator data as defined in a `stellar.toml` file.
  * @param alias A name for display in stellar-core configs
  * @param displayName A human-readable name for display in quorum explorers and other interfaces
  * @param publicKey The Stellar account associated with the node
  * @param host The IP:port or domain:port peers can use to connect to the node
  * @param history The location of the history archive published by this validator
  */
case class Validator(alias: Option[String] = None,
                     displayName: Option[String] = None,
                     publicKey: Option[PublicKey] = None,
                     host: Option[String] = None,
                     history: Option[HttpUrl] = None)

object Validator extends TomlParsers {

  def parse(tbl: Tbl): Validator = {
    def parseTomlValue[T](key: String, parser: PartialFunction[Value, T]) =
      super.parseTomlValue(tbl, key, parser)

    Validator(
      alias = parseTomlValue("ALIAS", string),
      displayName = parseTomlValue("DISPLAY_NAME", string),
      publicKey = parseTomlValue("PUBLIC_KEY", publicKey),
      host = parseTomlValue("HOST", string),
      history = parseTomlValue("HISTORY", url)
    )
  }
}
package stellar.sdk.model.response

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject

/**
  * Information on the network, as provided by the Horizon root document.
  */
case class NetworkInfo(horizonVersion: String,
                       coreVersion: String,
                       earliestLedger: Long,
                       latestLedger: Long,
                       passphrase: String,
                       currentProtocolVersion: Int,
                       supportedProtocolVersion: Int)

object NetworkInfoDeserializer extends ResponseParser[NetworkInfo]({ o: JObject =>
  implicit val formats = DefaultFormats

  NetworkInfo(
    horizonVersion = (o \ "horizon_version").extract[String],
    coreVersion = (o \ "core_version").extract[String],
    earliestLedger = (o \ "history_elder_ledger").extract[Long],
    latestLedger = (o \ "history_latest_ledger").extract[Long],
    passphrase = (o \ "network_passphrase").extract[String],
    currentProtocolVersion = (o \ "current_protocol_version").extract[Int],
    supportedProtocolVersion = (o \ "core_supported_protocol_version").extract[Int]
  )
})
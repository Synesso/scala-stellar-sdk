package stellar.sdk.resp

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}

case class TransactionResp(hash: String, ledger: Long, envelopeXDR: String, resultXDR: String, resultMetaXDR: String)

object TransactionRespDeserializer extends CustomSerializer[TransactionResp](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats

    import org.json4s.native.JsonMethods._
    println(pretty(render(o)))

    TransactionResp(
      hash = (o \ "hash").extract[String],
      ledger = (o \ "ledger").extract[Long],
      envelopeXDR = (o \ "envelope_xdr").extract[String],
      resultXDR = (o \ "result_xdr").extract[String],
      resultMetaXDR = (o \ "result_meta_xdr").extract[String]
    )

}, PartialFunction.empty)
)


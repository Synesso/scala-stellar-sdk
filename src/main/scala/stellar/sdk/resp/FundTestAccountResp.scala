package stellar.sdk.resp

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._

import scala.util.Try

case class FundTestAccountResp(hash: String, ledger: Long)

object FundTestAccountResp {
  implicit val formats = DefaultFormats
  def apply(json: String): Try[FundTestAccountResp] = Try {
    parse(json).extract[FundTestAccountResp]
  }
}

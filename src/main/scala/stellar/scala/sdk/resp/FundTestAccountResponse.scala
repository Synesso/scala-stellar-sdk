package stellar.scala.sdk.resp

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._

import scala.util.Try

case class FundTestAccountResponse(hash: String, ledger: Long)

object FundTestAccountResponse {
  implicit val formats = DefaultFormats
  def apply(json: String): Try[FundTestAccountResponse] = Try {
    parse(json).extract[FundTestAccountResponse]
  }
}

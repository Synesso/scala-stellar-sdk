package stellar.sdk.inet

import com.softwaremill.sttp.Uri
import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.stellar.sdk.xdr.TransactionResult
import stellar.sdk.resp.{TxResult, TxnFailureDeserializer}

import scala.util.Try

case class TxnFailure(uri: Uri, status: Int, detail: String, resultCode: Option[String],
                      operationResultCodes: Option[Array[String]], resultXDR: Option[String])
  extends Exception(s"Uri: $uri - $detail${resultCode.map(": " + _).getOrElse("")}${
    operationResultCodes.map(_.mkString(",")).map(" - " + _).getOrElse("")
  }") {

  def result: Option[TransactionResult] = resultXDR.map(TxResult.decodeXDR)
}

object TxnFailure {
  implicit val formats = Serialization.formats(NoTypeHints) + TxnFailureDeserializer

  def apply(uri: Uri, s: String): Try[TxnFailure] = Try {
    parse(s).extract[TxnFailure].copy(uri = uri)
  }
}

package stellar.sdk.inet

import akka.http.scaladsl.model.Uri
import org.stellar.sdk.xdr.TransactionResult
import stellar.sdk.resp.TxResult

case class TxnFailure(uri: Uri, status: Int, detail: String, resultCode: Option[String],
                      operationResultCodes: Option[Array[String]], resultXDR: Option[String])
  extends Exception(s"Uri: $uri - $detail${resultCode.map(": " + _).getOrElse("")}${
    operationResultCodes.map(_.mkString(",")).map(" - " + _).getOrElse("")
  }. Result XDR = $resultXDR") {

  def result: Option[TransactionResult] = resultXDR.map(TxResult.decodeXDR)
}

package stellar.sdk.resp

import org.stellar.sdk.xdr.TransactionResult
import stellar.sdk.XDRPrimitives

import scala.util.Try

object TxResult {

  def decodeXDR(base64: String): Try[TransactionResult] = {
    Try(TransactionResult.decode(XDRPrimitives.inputStream(base64)))
  }
}

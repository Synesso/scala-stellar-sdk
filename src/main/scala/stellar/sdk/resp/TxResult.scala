package stellar.sdk.resp

import org.stellar.sdk.xdr.{TransactionMeta, TransactionResult}
import stellar.sdk.XDRPrimitives

import scala.util.Try

object TxResult {

  def decodeXDR(base64: String): Try[TransactionResult] = {
    Try(TransactionResult.decode(XDRPrimitives.inputStream(base64)))
  }

  def decodeMetaXDR(base64: String): Try[TransactionMeta] = {
    Try(TransactionMeta.decode(XDRPrimitives.inputStream(base64)))
  }
}

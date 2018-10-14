package stellar.sdk.resp

import org.stellar.sdk.xdr.{TransactionMeta, TransactionResult}
import stellar.sdk.XDRPrimitives

object TxResult {

  def decodeXDR(base64: String): TransactionResult = {
    TransactionResult.decode(XDRPrimitives.inputStream(base64))
  }

  def decodeMetaXDR(base64: String): TransactionMeta = {
    TransactionMeta.decode(XDRPrimitives.inputStream(base64))
  }
}

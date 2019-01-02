package stellar.sdk.model.response

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk.model.result._
import stellar.sdk.{NativeAmount, Network, SignedTransaction}

sealed abstract class TransactionPostResponse(envelopeXDR: String, resultXDR: String) {
  val isSuccess: Boolean
  def transaction(implicit network: Network): SignedTransaction = SignedTransaction.decodeXDR(envelopeXDR)
}

/**
  *  The data returned from Horizon when a transaction post was accepted into a ledger.
  */
case class TransactionApproved(hash: String, ledger: Long,
                               envelopeXDR: String, resultXDR: String, resultMetaXDR: String)
  extends TransactionPostResponse(envelopeXDR, resultXDR) {

  override val isSuccess: Boolean = true

  // -- unroll nested XDR deserialised object into this object for convenience
  lazy val result: TransactionSuccess = TransactionResult.decodeXDR(resultXDR).asInstanceOf[TransactionSuccess]

  def feeCharged: NativeAmount = result.feeCharged

  def operationResults: Seq[OperationResult] = result.operationResults

}

/**
  *  The data returned from Horizon when a transaction post was rejected.
  */
case class TransactionRejected(status: Int, detail: String,
                               resultCode: String, opResultCodes: Seq[String],
                               envelopeXDR: String, resultXDR: String)
  extends TransactionPostResponse(envelopeXDR, resultXDR) {

  override val isSuccess: Boolean = false

  // -- unroll nested XDR deserialised object into this object for convenience
  lazy val result: TransactionNotSuccessful = TransactionResult.decodeXDR(resultXDR).asInstanceOf[TransactionNotSuccessful]

  def feeCharged: NativeAmount = result match {
    case TransactionFailure(fee, _) => fee
    case _ => NativeAmount(0)
  }
}



object TransactionPostResponseDeserializer extends ResponseParser[TransactionPostResponse]({
  o: JObject =>
    implicit val formats = DefaultFormats

    (o \ "type").extractOpt[String] match {

      case Some("https://stellar.org/horizon-errors/transaction_failed") =>
        TransactionRejected(
          status = (o \ "status").extract[Int],
          detail = (o \ "detail").extract[String],
          resultCode = (o \ "extras" \ "result_codes" \ "transaction").extract[String],
          opResultCodes = (o \ "extras" \ "result_codes" \ "operations").extract[Seq[String]],
          resultXDR = (o \ "extras" \ "result_xdr").extract[String],
          envelopeXDR = (o \ "extras" \ "envelope_xdr").extract[String]
        )

      case _ =>
        TransactionApproved(
          hash = (o \ "hash").extract[String],
          ledger = (o \ "ledger").extract[Long],
          envelopeXDR = (o \ "envelope_xdr").extract[String],
          resultXDR = (o \ "result_xdr").extract[String],
          resultMetaXDR = (o \ "result_meta_xdr").extract[String]
        )
    }
})


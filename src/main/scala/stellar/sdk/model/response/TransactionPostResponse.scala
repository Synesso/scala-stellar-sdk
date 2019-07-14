package stellar.sdk.model.response

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk.Network
import stellar.sdk.model.ledger.TransactionLedgerEntries
import stellar.sdk.model.result._
import stellar.sdk.model.{NativeAmount, SignedTransaction}

sealed abstract class TransactionPostResponse(envelopeXDR: String, resultXDR: String) {
  val isSuccess: Boolean

  def transaction(implicit network: Network): SignedTransaction = SignedTransaction.decodeXDR(envelopeXDR)

  def feeCharged: NativeAmount

  /**
    * Whether the fee-paying account's sequence number was incremented. This is inferred to be true when the fee is
    * not zero.
    */
  def sequenceIncremented: Boolean = feeCharged.units != 0
}

/**
  *  The data returned from Horizon when a transaction post was accepted into a ledger.
  */
case class TransactionApproved(hash: String, ledger: Long,
                               envelopeXDR: String, resultXDR: String, resultMetaXDR: String)
  extends TransactionPostResponse(envelopeXDR, resultXDR) {

  override val isSuccess: Boolean = true

  // -- unroll nested XDR deserialised objects into this object for convenience
  lazy val result: TransactionSuccess = TransactionResult.decodeXDR(resultXDR).asInstanceOf[TransactionSuccess]
  lazy val ledgerEntries: TransactionLedgerEntries = TransactionLedgerEntries.decodeXDR(resultMetaXDR)

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

  def feeCharged: NativeAmount = result.feeCharged
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


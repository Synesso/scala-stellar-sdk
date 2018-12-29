package stellar.sdk.resp

import java.time.ZonedDateTime

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk.ByteArrays.base64
import stellar.sdk._
import stellar.sdk.res.{TransactionHistory, TransactionResult}

/*sealed trait TransactionSuccessResp extends TransactionPostResp {
  val hash: String
  val ledger: Long
  val resultMetaXDR: String

  /**
    * The previously submitted signed transaction as reported in the XDR returned from Horizon.
    */
  def transaction(implicit network: Network) = SignedTransaction.decodeXDR(envelopeXDR)

  /**
    * The transaction meta info as reported in the XDR returned from Horizon.
    * Note: This response provided is the native Java XDR type.
    */
  def resultMeta: TransactionMeta = TxResult.decodeMetaXDR(resultMetaXDR)
}
*/

/*
sealed trait TransactionPostResp {
  val envelopeXDR: String
  val resultXDR: String

  /**
    * The transaction result as reported in the XDR returned from Horizon.
    * Note: This response provided is the native Java XDR type.
    */
  def result: TransactionResult = TxResult.decodeXDR(resultXDR)
}
*/

/*
/**
  * The success response received after submitting a new transaction to Horizon
  */
case class TransactionProcessed(hash: String, ledger: Long, envelopeXDR: String, resultXDR: String, resultMetaXDR: String)
  extends TransactionSuccessResp with TransactionPostResp
*/

/*
/**
  * The failure response received after submitting a new transaction to Horizon
  */
case class TransactionRejected(status: Int, detail: String, envelopeXDR: String, resultXDR: String,
                               resultCode: String, operationResultCodes: Array[String])

  extends TransactionPostResp
*/


/*
/**
  * The response received when viewing historical transactions
  */
case class TransactionHistory(hash: String, ledger: Long, createdAt: ZonedDateTime, account: PublicKey,
                                  sequence: Long, feePaid: Int, operationCount: Int, memo: Memo, signatures: Seq[String],
                                  envelopeXDR: String, resultXDR: String, resultMetaXDR: String, feeMetaXDR: String)
  extends TransactionSuccessResp
*/


/*
object TransactionPostRespDeserializer extends ResponseParser[TransactionPostResp]({
  o: JObject =>
    implicit val formats = DefaultFormats

    (o \ "type").extractOpt[String] match {

      case Some("https://stellar.org/horizon-errors/transaction_failed") =>
        TransactionRejected(
          status = (o \ "status").extract[Int],
          detail = (o \ "detail").extract[String],
          resultCode = (o \ "extras" \ "result_codes" \ "transaction").extract[String],
          operationResultCodes = (o \ "extras" \ "result_codes" \ "operations").extract[Array[String]],
          resultXDR = (o \ "extras" \ "result_xdr").extract[String],
          envelopeXDR = (o \ "extras" \ "envelope_xdr").extract[String]
        )

      case _ =>
        TransactionProcessed(
          hash = (o \ "hash").extract[String],
          ledger = (o \ "ledger").extract[Long],
          envelopeXDR = (o \ "envelope_xdr").extract[String],
          resultXDR = (o \ "result_xdr").extract[String],
          resultMetaXDR = (o \ "result_meta_xdr").extract[String]
        )
    }
})
*/



package stellar.sdk.resp

import java.time.ZonedDateTime

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import org.stellar.sdk.xdr.{TransactionMeta, TransactionResult}
import stellar.sdk.ByteArrays.base64
import stellar.sdk._

sealed trait TransactionResp {
  val hash: String
  val ledger: Long
  val envelopeXDR: String
  val resultXDR: String
  val resultMetaXDR: String

  /**
    * The previously submitted signed transaction as reported in the XDR returned from Horizon.
    */
  def transaction(implicit network: Network) = SignedTransaction.decodeXDR(envelopeXDR)

  /**
    * The transaction result as reported in the XDR returned from Horizon.
    * Note: This response provided is the native Java XDR type.
    */
  def result: TransactionResult = TxResult.decodeXDR(resultXDR)

  /**
    * The transaction meta info as reported in the XDR returned from Horizon.
    * Note: This response provided is the native Java XDR type.
    */
  def resultMeta: TransactionMeta = TxResult.decodeMetaXDR(resultMetaXDR)
}

/**
  * The response received after submitting a new transaction to Horizon
  */
case class TransactionPostResp(hash: String, ledger: Long, envelopeXDR: String, resultXDR: String, resultMetaXDR: String)
  extends TransactionResp

/**
  * The response received when viewing historical transactions
  */
case class TransactionHistoryResp(hash: String, ledger: Long, createdAt: ZonedDateTime, account: PublicKey,
                                  sequence: Long, feePaid: Int, operationCount: Int, memo: Memo, signatures: Seq[String],
                                  envelopeXDR: String, resultXDR: String, resultMetaXDR: String, feeMetaXDR: String)
  extends TransactionResp

object TransactionPostRespDeserializer extends ResponseParser[TransactionPostResp]({
  o: JObject =>
    implicit val formats = DefaultFormats
    TransactionPostResp(
      hash = (o \ "hash").extract[String],
      ledger = (o \ "ledger").extract[Long],
      envelopeXDR = (o \ "envelope_xdr").extract[String],
      resultXDR = (o \ "result_xdr").extract[String],
      resultMetaXDR = (o \ "result_meta_xdr").extract[String]
    )
})

object TransactionHistoryRespDeserializer extends ResponseParser[TransactionHistoryResp]({
  o: JObject =>
    implicit val formats = DefaultFormats

    TransactionHistoryResp(
      hash = (o \ "hash").extract[String],
      ledger = (o \ "ledger").extract[Long],
      createdAt = ZonedDateTime.parse((o \ "created_at").extract[String]),
      account = KeyPair.fromAccountId((o \ "source_account").extract[String]),
      sequence = (o \ "source_account_sequence").extract[String].toLong,
      feePaid = (o \ "fee_paid").extract[Int],
      operationCount = (o \ "operation_count").extract[Int],
      memo = (o \ "memo_type").extract[String] match {
        case "none" => NoMemo
        case "id" => MemoId((o \ "memo").extract[String].toLong)
        case "text" => MemoText((o \ "memo").extractOpt[String].getOrElse(""))
        case "hash" => MemoHash(base64((o \ "memo").extract[String]))
      },
      signatures = (o \ "signatures").extract[Seq[String]], // todo - replace with Signature domain object after #13
      envelopeXDR = (o \ "envelope_xdr").extract[String],
      resultXDR = (o \ "result_xdr").extract[String],
      resultMetaXDR = (o \ "result_meta_xdr").extract[String],
      feeMetaXDR = (o \ "fee_meta_xdr").extract[String]
    )
})


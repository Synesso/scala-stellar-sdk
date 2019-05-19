package stellar.sdk.model.result

import java.time.ZonedDateTime

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk.model._
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.util.ByteArrays.base64
import stellar.sdk.{KeyPair, PublicKey}

/**
  * A transaction that has been included in the ledger sometime in the past.
  */
case class TransactionHistory(hash: String, ledgerId: Long, createdAt: ZonedDateTime, account: PublicKey,
                              sequence: Long, feePaid: NativeAmount, operationCount: Int, memo: Memo,
                              signatures: Seq[String], envelopeXDR: String, resultXDR: String, resultMetaXDR: String,
                              feeMetaXDR: String, validAfter: Option[ZonedDateTime], validBefore: Option[ZonedDateTime]) {

  lazy val result: TransactionResult = TransactionResult.decodeXDR(resultXDR)

}


object TransactionHistoryDeserializer extends ResponseParser[TransactionHistory]({
  o: JObject =>
    implicit val formats = DefaultFormats

    TransactionHistory(
      hash = (o \ "hash").extract[String],
      ledgerId = (o \ "ledger").extract[Long],
      createdAt = ZonedDateTime.parse((o \ "created_at").extract[String]),
      account = KeyPair.fromAccountId((o \ "source_account").extract[String]),
      sequence = (o \ "source_account_sequence").extract[String].toLong,
      feePaid = NativeAmount((o \ "fee_paid").extract[Int]),
      operationCount = (o \ "operation_count").extract[Int],
      memo = (o \ "memo_type").extract[String] match {
        case "none" => NoMemo
        case "id" => MemoId(BigInt((o \ "memo").extract[String]).toLong)
        case "text" => MemoText((o \ "memo").extractOpt[String].getOrElse(""))
        case "hash" => MemoHash(base64((o \ "memo").extract[String]))
        case "return" => MemoReturnHash(base64((o \ "memo").extract[String]))
      },
      signatures = (o \ "signatures").extract[Seq[String]],
      envelopeXDR = (o \ "envelope_xdr").extract[String],
      resultXDR = (o \ "result_xdr").extract[String],
      resultMetaXDR = (o \ "result_meta_xdr").extract[String],
      feeMetaXDR = (o \ "fee_meta_xdr").extract[String],
      validAfter = (o \ "valid_after").extractOpt[String].map(ZonedDateTime.parse),
      validBefore = (o \ "valid_before").extractOpt[String].map(ZonedDateTime.parse),
    )
})
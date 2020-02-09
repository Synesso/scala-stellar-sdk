package stellar.sdk.model.result

import java.time.ZonedDateTime

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import stellar.sdk.model._
import stellar.sdk.model.ledger.TransactionLedgerEntries.arr
import stellar.sdk.model.ledger.{LedgerEntryChange, LedgerEntryChanges, TransactionLedgerEntries}
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.util.ByteArrays.base64
import stellar.sdk.{KeyPair, PublicKey}

import scala.util.Try

/**
  * A transaction that has been included in the ledger sometime in the past.
  */
case class TransactionHistory(hash: String, ledgerId: Long, createdAt: ZonedDateTime, account: PublicKey,
                              sequence: Long, maxFee: NativeAmount, feeCharged: NativeAmount, operationCount: Int,
                              memo: Memo, signatures: Seq[String], envelopeXDR: String, resultXDR: String,
                              resultMetaXDR: String, feeMetaXDR: String, validAfter: Option[ZonedDateTime],
                              validBefore: Option[ZonedDateTime]) {

  lazy val result: TransactionResult = TransactionResult.decodeXDR(resultXDR)

  def ledgerEntries: TransactionLedgerEntries = TransactionLedgerEntries.decodeXDR(resultMetaXDR)
  def feeLedgerEntries: Seq[LedgerEntryChange] = LedgerEntryChanges.decodeXDR(feeMetaXDR)

  @deprecated("Replaced by `feeCharged`", "v0.7.2")
  val feePaid: NativeAmount = feeCharged

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
      maxFee = NativeAmount((o \ "max_fee").extract[Int]),
      feeCharged = NativeAmount((o \ "fee_charged").extract[Int]),
      operationCount = (o \ "operation_count").extract[Int],
      memo = (o \ "memo_type").extract[String] match {
        case "none" => NoMemo
        case "id" => MemoId(BigInt((o \ "memo").extract[String]).toLong)
        case "text" => MemoText((o \ "memo").extractOpt[String].getOrElse(""))
        case "hash" => MemoHash(base64((o \ "memo").extract[String]).toIndexedSeq)
        case "return" => MemoReturnHash(base64((o \ "memo").extract[String]).toIndexedSeq)
      },
      signatures = (o \ "signatures").extract[Seq[String]],
      envelopeXDR = (o \ "envelope_xdr").extract[String],
      resultXDR = (o \ "result_xdr").extract[String],
      resultMetaXDR = (o \ "result_meta_xdr").extract[String],
      feeMetaXDR = (o \ "fee_meta_xdr").extract[String],
      // TODO (jem) - Remove the Try wrappers when https://github.com/stellar/go/issues/1381 is fixed.
      validBefore = Try((o \ "valid_before").extractOpt[String].map(ZonedDateTime.parse)).getOrElse(None),
      validAfter = Try((o \ "valid_after").extractOpt[String].map(ZonedDateTime.parse)).getOrElse(None)
    )
})
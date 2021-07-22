package stellar.sdk.model.result

import okio.ByteString
import org.json4s.JsonAST.JObject
import org.json4s.{DefaultFormats, Formats}
import stellar.sdk.Network
import stellar.sdk.model._
import stellar.sdk.model.ledger.{LedgerEntryChange, LedgerEntryChanges, TransactionLedgerEntries}
import stellar.sdk.model.response.ResponseParser

import java.time.ZonedDateTime
import scala.util.Try

/**
 * A transaction that has been included in the ledger sometime in the past.
 */
case class TransactionHistory(
  hash: String,
  ledgerId: Long,
  createdAt: ZonedDateTime,
  account: AccountId,
  sequence: Long,
  maxFee: NativeAmount,
  feeCharged: NativeAmount,
  operationCount: Int,
  memo: Memo,
  signatures: Seq[String],
  envelopeXDR: String,
  resultXDR: String,
  resultMetaXDR: String,
  feeMetaXDR: String,
  validAfter: Option[ZonedDateTime],
  validBefore: Option[ZonedDateTime],
  feeBump: Option[FeeBumpHistory]
) {

  lazy val result: TransactionResult = TransactionResult.decodeXdrString(resultXDR)

  def ledgerEntries: TransactionLedgerEntries = TransactionLedgerEntries.decodeXDR(resultMetaXDR)

  def feeLedgerEntries: Seq[LedgerEntryChange] = LedgerEntryChanges.decodeXDR(feeMetaXDR)

  def transaction(network: Network): Transaction = Transaction.decodeXdrString(envelopeXDR)(network)

  @deprecated("Replaced by `feeCharged`", "v0.7.2")
  val feePaid: NativeAmount = feeCharged

}


object TransactionHistoryDeserializer extends {
} with ResponseParser[TransactionHistory]({
  o: JObject =>
    implicit val formats: Formats = DefaultFormats

    val maxFee = NativeAmount((o \ "max_fee").extract[String].toInt)
    val signatures = (o \ "signatures").extract[List[String]]
    val hash = (o \ "hash").extract[String]

    val inner = for {
      hash <- (o \ "inner_transaction" \ "hash").extractOpt[String]
      maxFee <- (o \ "inner_transaction" \ "max_fee").extractOpt[Int].map(NativeAmount(_))
      signatures <- (o \ "inner_transaction" \ "signatures").extractOpt[List[String]]
    } yield (hash, maxFee, signatures)
    TransactionHistory(
      hash = inner.map(_._1).getOrElse(hash),
      ledgerId = (o \ "ledger").extract[Long],
      createdAt = ZonedDateTime.parse((o \ "created_at").extract[String]),
      account = AccountId.parse(o, "source_account"),
      sequence = (o \ "source_account_sequence").extract[String].toLong,
      maxFee = inner.map(_._2).getOrElse(maxFee),
      feeCharged = NativeAmount((o \ "fee_charged").extract[String].toInt),
      operationCount = (o \ "operation_count").extract[Int],
      memo = (o \ "memo_type").extract[String] match {
        case "none" => NoMemo
        case "id" => MemoId(BigInt((o \ "memo").extract[String]).toLong)
        case "text" =>
          (o \ "memo_bytes").extractOpt[String]
            .map(ByteString.decodeBase64).map(MemoText(_))
            .getOrElse(MemoText((o \ "memo").extractOpt[String].getOrElse("")))
        case "hash" => MemoHash(ByteString.decodeBase64((o \ "memo").extract[String]))
        case "return" => MemoReturnHash(ByteString.decodeBase64((o \ "memo").extract[String]))
      },
      signatures = inner.map(_._3).getOrElse(signatures),
      envelopeXDR = (o \ "envelope_xdr").extract[String],
      resultXDR = (o \ "result_xdr").extract[String],
      resultMetaXDR = (o \ "result_meta_xdr").extract[String],
      feeMetaXDR = (o \ "fee_meta_xdr").extract[String],
      // TODO (jem) - Remove the Try wrappers when https://github.com/stellar/go/issues/1381 is fixed.
      validBefore = Try((o \ "valid_before").extractOpt[String].map(ZonedDateTime.parse)).getOrElse(None),
      validAfter = Try((o \ "valid_after").extractOpt[String].map(ZonedDateTime.parse)).getOrElse(None),
      feeBump = inner.map { _ => FeeBumpHistory(maxFee, hash, signatures) }
    )
})
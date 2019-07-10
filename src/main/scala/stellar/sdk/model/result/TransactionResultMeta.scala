package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.xdr.Decode
import stellar.sdk.util.ByteArrays

case class TransactionResultMeta(ledgerEntryChanges: Seq[LedgerEntryChange])

object TransactionResultMeta {

  def decodeXDR(base64: String) = decode.run(ByteArrays.base64(base64))

  def decode: State[Seq[Byte], TransactionResultMeta] = for {
    version <- Decode.int
    meta <- versions(version)
  } yield meta

  private def decodeV0: State[Seq[Byte], TransactionResultMeta] = for {
    ops <- Decode.arr(LedgerEntryChange.decode)
  } yield TransactionResultMeta(ops)

  private def decodeV1: State[Seq[Byte], TransactionResultMeta] = ???

  private val versions = Map(
    0 -> decodeV0,
    1 -> decodeV1)

}


case class LedgerEntryChange()

object LedgerEntryChange {

  def decode: State[Seq[Byte], LedgerEntryChange] = for {

  } yield LedgerEntryChange()

}
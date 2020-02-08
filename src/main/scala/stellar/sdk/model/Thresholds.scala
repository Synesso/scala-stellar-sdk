package stellar.sdk.model

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable}
import stellar.sdk.model.xdr.Encode._

/**
  * The thresholds for operations on this account.
  * @param low The weight required for a valid transaction including the Allow Trust and Bump Sequence operations.
  * @param med The weight required for a valid transaction including the Create Account, Payment, Path Payment, Manage
  *            Buy Offer, Manage Sell Offer, Create Passive Sell Offer, Change Trust, Inflation, and Manage Data operations.
  * @param high The weight required for a valid transaction including the Account Merge and Set Options operations.
  */
case class Thresholds(low: Int, med: Int, high: Int)

/**
  * The thresholds for operations on this account, as described in transaction meta data for ledger effects.
  * This differs from @see[[Thresholds]] in that it also contains the master weight for the account's primary signature.
  *
  * @param master The weight provided by the primary signature for this account.
  * @param low The weight required for a valid transaction including the Allow Trust and Bump Sequence operations.
  * @param med The weight required for a valid transaction including the Create Account, Payment, Path Payment, Manage
  *            Buy Offer, Manage Sell Offer, Create Passive Sell Offer, Change Trust, Inflation, and Manage Data operations.
  * @param high The weight required for a valid transaction including the Account Merge and Set Options operations.
  */
case class LedgerThresholds(master: Int, low: Int, med: Int, high: Int) extends Encodable {
  override def encode: LazyList[Byte] = bytes(4, Array[Byte](master.toByte, low.toByte, med.toByte, high.toByte))
}

object LedgerThresholds extends Decode {
  val decode: State[Seq[Byte], LedgerThresholds] = bytes(4).map { bs =>
    val Seq(master, low, med, high): Seq[Int] = bs.map(_ & 0xff)
    LedgerThresholds(master, low, med, high)
  }
}
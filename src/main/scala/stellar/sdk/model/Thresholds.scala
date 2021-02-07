package stellar.sdk.model

import org.stellar.xdr.{Thresholds => XThresholds}

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
case class LedgerThresholds(master: Int, low: Int, med: Int, high: Int) {
  def xdr: XThresholds = new XThresholds(Array(master, low, med, high).map(_.toByte))
}

object LedgerThresholds {
  def decodeXdr(xdr: XThresholds): LedgerThresholds = {
    val Array(master, low, med, high) = xdr.getThresholds.map(_.toInt & 0xFF)
    LedgerThresholds(master, low, med, high)
  }
}
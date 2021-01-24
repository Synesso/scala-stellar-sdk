package stellar.sdk.model.ledger

import org.stellar.xdr.LedgerKey._
import org.stellar.xdr.{Int64, LedgerEntryType, String64, XdrString, LedgerKey => XLedgerKey}
import stellar.sdk.PublicKeyOps
import stellar.sdk.model.{AccountId, Asset, ClaimableBalanceId, NonNativeAsset}

sealed trait LedgerKey {
  def xdr: XLedgerKey
}

object LedgerKey {
  def decode(xdr: XLedgerKey): LedgerKey = {
    xdr.getDiscriminant match {
      case LedgerEntryType.ACCOUNT =>
        AccountKey(AccountId.decode(xdr.getAccount.getAccountID).publicKey)
      case LedgerEntryType.CLAIMABLE_BALANCE =>
        ClaimableBalanceKey(ClaimableBalanceId.decode(xdr.getClaimableBalance.getBalanceID))
      case LedgerEntryType.DATA =>
        DataKey(
          account = AccountId.decode(xdr.getData.getAccountID).publicKey,
          name = xdr.getData.getDataName.getString64.toString
        )
      case LedgerEntryType.OFFER =>
        OfferKey(
          account = AccountId.decode(xdr.getOffer.getSellerID).publicKey,
          offerId = xdr.getOffer.getOfferID.getInt64
        )
      case LedgerEntryType.TRUSTLINE =>
        TrustLineKey(
          account = AccountId.decode(xdr.getTrustLine.getAccountID).publicKey,
          asset = Asset.decode(xdr.getTrustLine.getAsset).asInstanceOf[NonNativeAsset]
        )
    }
  }
}

case class AccountKey(account: PublicKeyOps) extends LedgerKey {
  def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.ACCOUNT)
    .account(new LedgerKeyAccount.Builder()
      .accountID(account.toAccountId.accountIdXdr)
      .build())
    .build()
}

case class TrustLineKey(account: PublicKeyOps, asset: NonNativeAsset) extends LedgerKey {
  def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.TRUSTLINE)
    .trustLine(new LedgerKeyTrustLine.Builder()
      .accountID(account.toAccountId.accountIdXdr)
      .build())
    .build()
}

case class OfferKey(account: PublicKeyOps, offerId: Long) extends LedgerKey {
  def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.OFFER)
    .offer(new LedgerKeyOffer.Builder()
      .offerID(new Int64(offerId))
      .sellerID(account.toAccountId.accountIdXdr)
      .build())
    .build()
}

case class DataKey(account: PublicKeyOps, name: String) extends LedgerKey {
  def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.DATA)
    .data(new LedgerKeyData.Builder()
      .dataName(new String64(new XdrString(name)))
      .build())
    .build()
}

case class ClaimableBalanceKey(id: ClaimableBalanceId) extends LedgerKey {
  def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.CLAIMABLE_BALANCE)
    .claimableBalance(new LedgerKeyClaimableBalance.Builder()
      .balanceID(id.xdr)
      .build())
    .build()
}

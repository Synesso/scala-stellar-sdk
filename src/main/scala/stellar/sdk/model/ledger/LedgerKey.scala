package stellar.sdk.model.ledger

import org.stellar.xdr.LedgerKey.{LedgerKeyClaimableBalance, LedgerKeyData, LedgerKeyOffer, LedgerKeyTrustLine}
import org.stellar.xdr.{Int64, LedgerEntryType, String64, XdrString, LedgerKey => XLedgerKey}
import stellar.sdk.PublicKeyOps
import stellar.sdk.model.{AccountId, Asset, ClaimableBalanceId, NonNativeAsset}

sealed trait LedgerKey {
  def xdr: XLedgerKey
}

object LedgerKey {
  def decodeXdr(xdr: XLedgerKey): LedgerKey = {
    xdr.getDiscriminant match {
      case LedgerEntryType.ACCOUNT =>
        AccountKey(AccountId.decodeXdr(xdr.getAccount.getAccountID).publicKey)
      case LedgerEntryType.CLAIMABLE_BALANCE =>
        ClaimableBalanceKey(ClaimableBalanceId.decodeXdr(xdr.getClaimableBalance.getBalanceID))
      case LedgerEntryType.DATA =>
        DataKey(
          account = AccountId.decodeXdr(xdr.getData.getAccountID).publicKey,
          name = xdr.getData.getDataName.getString64.toString
        )
      case LedgerEntryType.OFFER =>
        OfferKey(
          account = AccountId.decodeXdr(xdr.getOffer.getSellerID).publicKey,
          offerId = xdr.getOffer.getOfferID.getInt64
        )
      case LedgerEntryType.TRUSTLINE =>
        TrustLineKey(
          account = AccountId.decodeXdr(xdr.getTrustLine.getAccountID).publicKey,
          asset = Asset.decodeXdr(xdr.getTrustLine.getAsset).asInstanceOf[NonNativeAsset]
        )
    }
  }
}

case class AccountKey(account: PublicKeyOps) extends LedgerKey {
  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.ACCOUNT)
    .account(new XLedgerKey.LedgerKeyAccount.Builder()
      .accountID(account.toAccountId.xdr)
      .build())
    .build()
}

case class TrustLineKey(account: PublicKeyOps, asset: NonNativeAsset) extends LedgerKey {
  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.TRUSTLINE)
    .trustLine(new LedgerKeyTrustLine.Builder()
      .accountID(account.toAccountId.xdr)
      .asset(asset.xdr)
      .build())
    .build()
}

case class OfferKey(account: PublicKeyOps, offerId: Long) extends LedgerKey {
  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.OFFER)
    .offer(new LedgerKeyOffer.Builder()
      .offerID(new Int64(offerId))
      .sellerID(account.toAccountId.xdr)
      .build())
    .build()
}

case class DataKey(account: PublicKeyOps, name: String) extends LedgerKey {
  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.DATA)
    .data(new LedgerKeyData.Builder()
      .accountID(account.toAccountId.xdr)
      .dataName(new String64(new XdrString(name)))
      .build())
    .build()
}

case class ClaimableBalanceKey(id: ClaimableBalanceId) extends LedgerKey {
  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.CLAIMABLE_BALANCE)
    .claimableBalance(new LedgerKeyClaimableBalance.Builder()
      .balanceID(id.xdr)
      .build())
    .build()
}

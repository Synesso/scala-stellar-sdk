package stellar.sdk.model.ledger

import cats.data.State
import org.stellar.xdr.LedgerKey.{LedgerKeyClaimableBalance, LedgerKeyData, LedgerKeyOffer, LedgerKeyTrustLine}
import org.stellar.xdr.{Int64, LedgerEntryType, String64, XdrString, LedgerKey => XLedgerKey}
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.model.{AccountId, Asset, ClaimableBalanceId, NonNativeAsset}
import stellar.sdk.{KeyPair, PublicKeyOps}

sealed trait LedgerKey extends Encodable {
  def xdr: XLedgerKey
}

object LedgerKey extends Decode {
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


  val decode: State[Seq[Byte], LedgerKey] = switch[LedgerKey](
    widen(AccountKey.decode),
    widen(TrustLineKey.decode),
    widen(OfferKey.decode),
    widen(DataKey.decode),
    widen(ClaimableBalanceKey.decode)
  )
}

case class AccountKey(account: PublicKeyOps) extends LedgerKey {
  override def encode: LazyList[Byte] = Encode.int(0) ++ account.encode

  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.ACCOUNT)
    .account(new XLedgerKey.LedgerKeyAccount.Builder()
      .accountID(account.toAccountId.xdr)
      .build())
    .build()
}

object AccountKey extends Decode {
  val decode: State[Seq[Byte], AccountKey] = KeyPair.decode.map(AccountKey(_))
}

case class TrustLineKey(account: PublicKeyOps, asset: NonNativeAsset) extends LedgerKey {
  override def encode: LazyList[Byte] = Encode.int(1) ++ account.encode ++ asset.encode

  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.TRUSTLINE)
    .trustLine(new LedgerKeyTrustLine.Builder()
      .accountID(account.toAccountId.xdr)
      .asset(asset.xdr)
      .build())
    .build()
}

object TrustLineKey extends Decode {
  val decode: State[Seq[Byte], TrustLineKey] = for {
    account <- KeyPair.decode
    asset <- Asset.decode.map(_.asInstanceOf[NonNativeAsset])
  } yield TrustLineKey(account, asset)
}

case class OfferKey(account: PublicKeyOps, offerId: Long) extends LedgerKey {
  override def encode: LazyList[Byte] = Encode.int(2) ++ account.encode ++ Encode.long(offerId)

  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.OFFER)
    .offer(new LedgerKeyOffer.Builder()
      .offerID(new Int64(offerId))
      .sellerID(account.toAccountId.xdr)
      .build())
    .build()
}

object OfferKey extends Decode {
  val decode: State[Seq[Byte], OfferKey] = for {
    account <- KeyPair.decode
    offerId <- long
  } yield OfferKey(account, offerId)
}

case class DataKey(account: PublicKeyOps, name: String) extends LedgerKey {
  override def encode: LazyList[Byte] = Encode.int(3) ++ account.encode ++ Encode.string(name)

  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.DATA)
    .data(new LedgerKeyData.Builder()
      .accountID(account.toAccountId.xdr)
      .dataName(new String64(new XdrString(name)))
      .build())
    .build()
}

object DataKey extends Decode {
  val decode: State[Seq[Byte], DataKey] = for {
    account <- KeyPair.decode
    name <- string
  } yield DataKey(account, name)
}

case class ClaimableBalanceKey(id: ClaimableBalanceId) extends LedgerKey {
  override def encode: LazyList[Byte] = Encode.int(4) ++ id.encode

  override def xdr: XLedgerKey = new XLedgerKey.Builder()
    .discriminant(LedgerEntryType.CLAIMABLE_BALANCE)
    .claimableBalance(new LedgerKeyClaimableBalance.Builder()
      .balanceID(id.xdr)
      .build())
    .build()
}

object ClaimableBalanceKey extends Decode {
  val decode: State[Seq[Byte], ClaimableBalanceKey] = ClaimableBalanceId.decode.map(ClaimableBalanceKey(_))
}

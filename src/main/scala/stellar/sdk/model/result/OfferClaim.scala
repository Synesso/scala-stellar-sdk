package stellar.sdk.model.result

import org.stellar.xdr.{ClaimOfferAtom, Int64}
import stellar.sdk.PublicKey
import stellar.sdk.model.{AccountId, Amount, Asset}

case class OfferClaim(seller: PublicKey, offerId: Long, sold: Amount, bought: Amount) {
  def xdr: ClaimOfferAtom = new ClaimOfferAtom.Builder()
    .amountBought(new Int64(bought.units))
    .amountSold(new Int64(sold.units))
    .assetBought(bought.asset.xdr)
    .assetSold(sold.asset.xdr)
    .offerID(new Int64(offerId))
    .sellerID(seller.toAccountId.xdr)
    .build()
}

object OfferClaim {
  def decodeXdr(xdr: ClaimOfferAtom): OfferClaim = OfferClaim(
    seller = AccountId.decodeXdr(xdr.getSellerID).publicKey,
    offerId = xdr.getOfferID.getInt64,
    sold = Amount(
      xdr.getAmountSold.getInt64,
      Asset.decodeXdr(xdr.getAssetSold)
    ),
    bought = Amount(
      xdr.getAmountBought.getInt64,
      Asset.decodeXdr(xdr.getAssetBought)
    )
  )
}
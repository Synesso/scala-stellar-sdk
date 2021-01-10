package stellar.sdk.model.result

import org.stellar.xdr.{ClaimOfferAtom, Int64}
import stellar.sdk.PublicKey
import stellar.sdk.model.Amount

case class OfferClaim(seller: PublicKey, offerId: Long, sold: Amount, bought: Amount) {
  def xdr: ClaimOfferAtom = new ClaimOfferAtom.Builder()
    .amountBought(new Int64(bought.units))
    .amountSold(new Int64(sold.units))
    .assetBought(bought.asset.xdr)
    .assetSold(sold.asset.xdr)
    .offerID(new Int64(offerId))
    .sellerID(seller.toAccountId.accountIdXdr)
    .build()
}

object OfferClaim {
}
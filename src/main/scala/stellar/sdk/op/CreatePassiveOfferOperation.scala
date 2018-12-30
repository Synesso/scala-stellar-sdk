package stellar.sdk.op

import cats.data.State
import stellar.sdk.xdr.{Decode, Encode}
import stellar.sdk.{Amount, Asset, _}

/**
  * Creates an offer that won’t consume a counter offer that exactly matches this offer.
  *
  * @param selling the total amount of tokens being offered
  * @param buying the asset being sought
  * @param price the price the offerer is willing to accept
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#create-passive-offer endpoint doc]]
  */
case class CreatePassiveOfferOperation(selling: Amount, buying: Asset, price: Price,
                                       sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(4) ++
      selling.asset.encode ++
      buying.encode ++
      Encode.long(selling.units) ++
      price.encode
}

object CreatePassiveOfferOperation {
  val decode: State[Seq[Byte], CreatePassiveOfferOperation] = for {
    sellingAsset <- Asset.decode
    buyingAsset <- Asset.decode
    sellingUnits <- Decode.long
    price <- Price.decode
  } yield CreatePassiveOfferOperation(Amount(sellingUnits, sellingAsset), buyingAsset, price)
}

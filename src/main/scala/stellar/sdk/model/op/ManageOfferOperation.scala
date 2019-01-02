package stellar.sdk.model.op

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encode}
import stellar.sdk.{Amount, Asset, _}

import scala.util.Try

sealed trait ManageOfferOperation extends Operation {
  val offerId: Long = 0
}

/**
  * Creates an offer in the Stellar network.
  */
case class CreateOfferOperation(selling: Amount, buying: Asset, price: Price,
                                sourceAccount: Option[PublicKeyOps] = None) extends ManageOfferOperation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(3) ++
      selling.asset.encode ++
      buying.encode ++
      Encode.long(selling.units) ++
      price.encode ++
      Encode.long(0)
}

/**
  * Deletes an offer in the Stellar network.
  *
  * @param offerId the id of the offer to be deleted
  * @param selling the asset being offered
  * @param buying the asset previously sought
  * @param price the price being offered
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-offer endpoint doc]]
  */
case class DeleteOfferOperation(override val offerId: Long,
                                selling: Asset, buying: Asset, price: Price,
                                sourceAccount: Option[PublicKeyOps] = None) extends ManageOfferOperation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(3) ++
      selling.encode ++
      buying.encode ++
      Encode.long(0) ++
      price.encode ++
      Encode.long(offerId)
}

/**
  * Updates an offer in the Stellar network.
  *
  * @param offerId the id of the offer to be modified
  * @param selling the asset and amount being offered
  * @param buying the asset sought
  * @param price the price being offered
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-offer endpoint doc]]
  */
case class UpdateOfferOperation(override val offerId: Long,
                                selling: Amount, buying: Asset, price: Price,
                                sourceAccount: Option[PublicKeyOps] = None) extends ManageOfferOperation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(3) ++
      selling.asset.encode ++
      buying.encode ++
      Encode.long(selling.units) ++
      price.encode ++
      Encode.long(offerId)
}

object ManageOfferOperation {
  def decode: State[Seq[Byte], ManageOfferOperation] = for {
    sellingAsset <- Asset.decode
    buyingAsset <- Asset.decode
    sellingUnits <- Decode.long
    price <- Price.decode
    offerId <- Decode.long
  } yield {
    if (offerId == 0) CreateOfferOperation(Amount(sellingUnits, sellingAsset), buyingAsset, price)
    else if (sellingUnits == 0) DeleteOfferOperation(offerId, sellingAsset, buyingAsset, price)
    else UpdateOfferOperation(offerId, Amount(sellingUnits, sellingAsset), buyingAsset, price)
  }
}

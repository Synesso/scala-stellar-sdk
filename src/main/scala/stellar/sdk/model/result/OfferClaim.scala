package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.Amount
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.{KeyPair, PublicKey}

case class OfferClaim(seller: PublicKey, offerId: Long, sold: Amount, bought: Amount) extends Encodable {
  def encode: Stream[Byte] = seller.encode ++ Encode.long(offerId) ++ sold.encode ++ bought.encode
}

object OfferClaim extends Decode {
  val decode: State[Seq[Byte], OfferClaim] = for {
    seller <- KeyPair.decode
    offerId <- long
    sold <- Amount.decode
    bought <- Amount.decode
  } yield OfferClaim(seller, offerId, sold, bought)
}
package stellar.sdk.result

import cats.data.State
import stellar.sdk.xdr.{Decode, Encodable, Encode}
import stellar.sdk.{Amount, KeyPair, PublicKey}

case class OfferClaim(seller: PublicKey, offerId: Long, sold: Amount, bought: Amount) extends Encodable {
  def encode: Stream[Byte] = seller.encode ++ Encode.long(offerId) ++ sold.encode ++ bought.encode
}

object OfferClaim {
  val decode: State[Seq[Byte], OfferClaim] = for {
    seller <- KeyPair.decode
    offerId <- Decode.long
    sold <- Amount.decode
    bought <- Amount.decode
  } yield OfferClaim(seller, offerId, sold, bought)
}
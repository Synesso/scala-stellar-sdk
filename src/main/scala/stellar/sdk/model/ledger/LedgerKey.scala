package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.{KeyPair, PublicKey, PublicKeyOps}
import stellar.sdk.model.{Asset, NonNativeAsset}
import stellar.sdk.model.xdr.{Decode, Encode}

sealed trait LedgerKey {
  def encode: Stream[Byte]
}

object LedgerKey extends Decode {

  val decode: State[Seq[Byte], LedgerKey] = switch[LedgerKey](
    widen(AccountKey.decode),
    widen(TrustLineKey.decode),
    widen(OfferKey.decode),
    widen(DataKey.decode)
  )
}

case class AccountKey(account: PublicKeyOps) extends LedgerKey {
  override def encode: Stream[Byte] = Encode.int(0) ++ account.encode
}

object AccountKey extends Decode {
  val decode: State[Seq[Byte], AccountKey] = KeyPair.decode.map(AccountKey(_))
}

case class TrustLineKey(account: PublicKeyOps, asset: NonNativeAsset) extends LedgerKey {
  override def encode: Stream[Byte] = Encode.int(1) ++ account.encode ++ asset.encode
}

object TrustLineKey extends Decode {
  val decode: State[Seq[Byte], TrustLineKey] = for {
    account <- KeyPair.decode
    asset <- Asset.decode.map(_.asInstanceOf[NonNativeAsset])
  } yield TrustLineKey(account, asset)
}

case class OfferKey(account: PublicKeyOps, offerId: Long) extends LedgerKey {
  override def encode: Stream[Byte] = Encode.int(2) ++ account.encode ++ Encode.long(offerId)
}

object OfferKey extends Decode {
  val decode: State[Seq[Byte], OfferKey] = for {
    account <- KeyPair.decode
    offerId <- long
  } yield OfferKey(account, offerId)
}

case class DataKey(account: PublicKeyOps, name: String) extends LedgerKey {
  override def encode: Stream[Byte] = Encode.int(3) ++ account.encode ++ Encode.string(name)
}

object DataKey extends Decode {
  val decode: State[Seq[Byte], DataKey] = for {
    account <- KeyPair.decode
    name <- string
  } yield DataKey(account, name)
}

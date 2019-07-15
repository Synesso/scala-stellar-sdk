package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.{KeyPair, PublicKey}
import stellar.sdk.model.{Asset, NonNativeAsset}
import stellar.sdk.model.xdr.Decode

sealed trait LedgerKey

object LedgerKey extends Decode {

  val decode: State[Seq[Byte], LedgerKey] = switch[LedgerKey](
    widen(AccountKey.decode),
    widen(TrustLineKey.decode),
    widen(OfferKey.decode),
    widen(DataKey.decode)
  )
}

case class AccountKey(account: PublicKey) extends LedgerKey

object AccountKey extends Decode {
  val decode: State[Seq[Byte], AccountKey] = KeyPair.decode.map(AccountKey(_))
}

case class TrustLineKey(account: PublicKey, asset: NonNativeAsset) extends LedgerKey

object TrustLineKey extends Decode {
  val decode: State[Seq[Byte], TrustLineKey] = for {
    account <- KeyPair.decode
    asset <- Asset.decode.map(_.asInstanceOf[NonNativeAsset])
  } yield TrustLineKey(account, asset)
}

case class OfferKey(account: PublicKey, offerId: Long) extends LedgerKey

object OfferKey extends Decode {
  val decode: State[Seq[Byte], OfferKey] = for {
    account <- KeyPair.decode
    offerId <- long
  } yield OfferKey(account, offerId)
}

case class DataKey(account: PublicKey, name: String) extends LedgerKey

object DataKey extends Decode {
  val decode: State[Seq[Byte], DataKey] = for {
    account <- KeyPair.decode
    name <- string
  } yield DataKey(account, name)
}

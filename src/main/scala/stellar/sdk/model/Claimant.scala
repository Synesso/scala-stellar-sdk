package stellar.sdk.model

import cats.data.State
import stellar.sdk.model.xdr.Encode.int
import stellar.sdk.model.xdr.{Decode, Encodable}
import stellar.sdk.{KeyPair, PublicKeyOps}

sealed trait Claimant extends Encodable

object Claimant extends Decode {

  val decode: State[Seq[Byte], Claimant] = for {
    _ <- int
    accountId <- KeyPair.decode
    predicate <- ClaimPredicate.decode
  } yield AccountIdClaimant(accountId, predicate)
}

case class AccountIdClaimant(
  accountId: PublicKeyOps,
  predicate: ClaimPredicate
) extends Claimant {
  override def encode: LazyList[Byte] = int(0) ++ accountId.encode ++ predicate.encode
}

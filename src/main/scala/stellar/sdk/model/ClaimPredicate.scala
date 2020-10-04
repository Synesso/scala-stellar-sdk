package stellar.sdk.model

import java.time.Instant

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}

sealed trait ClaimPredicate extends Encodable {
  def check(claimCreation: Instant, ledgerCloseTime: Instant): Boolean
}

object ClaimPredicate extends Decode {

  def decode: State[Seq[Byte], ClaimPredicate] = int.flatMap {
    case 0 => State.pure(Unconditional)
    case 1 => arr(decode).map { case Seq(left, right) => And(left, right) }
    case 2 => arr(decode).map { case Seq(left, right) => Or(left, right) }
    case 3 => decode.map(Not)
    case 4 => long.map(Instant.ofEpochSecond).map(AbsolutelyBefore)
    case 5 => long.map(SinceClaimCreation)
  }

  case object Unconditional extends ClaimPredicate {
    override def encode: LazyList[Byte] = Encode.int(0)
    override def check(claimCreation: Instant, instant: Instant): Boolean = true
  }

  case class And(left: ClaimPredicate, right: ClaimPredicate) extends ClaimPredicate {
    override def encode: LazyList[Byte] = Encode.int(1) ++ Encode.arr(List(left, right))
    override def check(claimCreation: Instant, instant: Instant): Boolean =
      left.check(claimCreation, instant) && right.check(claimCreation, instant)
  }

  case class Or(left: ClaimPredicate, right: ClaimPredicate) extends ClaimPredicate {
    override def encode: LazyList[Byte] = Encode.int(2) ++ Encode.arr(List(left, right))
    override def check(claimCreation: Instant, instant: Instant): Boolean =
      left.check(claimCreation, instant) || right.check(claimCreation, instant)
  }

  case class Not(predicate: ClaimPredicate) extends ClaimPredicate {
    override def encode: LazyList[Byte] = Encode.int(3) ++ predicate.encode
    override def check(claimCreation: Instant, instant: Instant): Boolean =
      !predicate.check(claimCreation, instant)
  }

  /** Will return true if ledger closeTime < instant */
  case class AbsolutelyBefore(instant: Instant) extends ClaimPredicate {
    override def encode: LazyList[Byte] = Encode.int(4) ++ Encode.long(instant.getEpochSecond)
    override def check(claimCreation: Instant, instant: Instant): Boolean = !instant.isAfter(this.instant)
  }

  /** Seconds since closeTime of the ledger in which the ClaimableBalanceEntry was created */
  case class SinceClaimCreation(seconds: Long) extends ClaimPredicate {
    require(seconds >= 0, "Seconds must be non-negative")
    override def encode: LazyList[Byte] = Encode.int(5) ++ Encode.long(seconds)
    override def check(claimCreation: Instant, instant: Instant): Boolean =
      instant.minusSeconds(seconds).isBefore(claimCreation)
  }
}

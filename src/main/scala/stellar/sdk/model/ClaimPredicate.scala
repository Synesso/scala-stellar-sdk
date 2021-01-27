package stellar.sdk.model

import java.time.{Instant, ZonedDateTime}

import cats.data.State
import org.json4s.JObject
import org.stellar.xdr.{ClaimPredicateType, Int64, ClaimPredicate => XClaimPredicate}
import stellar.sdk.model.ClaimPredicate.parseClaimPredicate
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}

sealed trait ClaimPredicate extends Encodable {
  def check(claimCreation: Instant, ledgerCloseTime: Instant): Boolean
  def xdr: XClaimPredicate
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
    override val xdr: XClaimPredicate = new XClaimPredicate.Builder()
      .discriminant(ClaimPredicateType.CLAIM_PREDICATE_UNCONDITIONAL)
      .build()
  }

  case class And(left: ClaimPredicate, right: ClaimPredicate) extends ClaimPredicate {
    override def encode: LazyList[Byte] = Encode.int(1) ++ Encode.arr(List(left, right))
    override def check(claimCreation: Instant, instant: Instant): Boolean =
      left.check(claimCreation, instant) && right.check(claimCreation, instant)
    override def xdr: XClaimPredicate = new XClaimPredicate.Builder()
      .discriminant(ClaimPredicateType.CLAIM_PREDICATE_AND)
      .andPredicates(Array(left.xdr, right.xdr))
      .build()
  }

  case class Or(left: ClaimPredicate, right: ClaimPredicate) extends ClaimPredicate {
    override def encode: LazyList[Byte] = Encode.int(2) ++ Encode.arr(List(left, right))
    override def check(claimCreation: Instant, instant: Instant): Boolean =
      left.check(claimCreation, instant) || right.check(claimCreation, instant)
    override def xdr: XClaimPredicate = new XClaimPredicate.Builder()
      .discriminant(ClaimPredicateType.CLAIM_PREDICATE_OR)
      .orPredicates(Array(left.xdr, right.xdr))
      .build()
  }

  case class Not(predicate: ClaimPredicate) extends ClaimPredicate {
    override def encode: LazyList[Byte] = Encode.int(3) ++ predicate.encode
    override def check(claimCreation: Instant, instant: Instant): Boolean =
      !predicate.check(claimCreation, instant)
    override def xdr: XClaimPredicate = new XClaimPredicate.Builder()
      .discriminant(ClaimPredicateType.CLAIM_PREDICATE_NOT)
      .notPredicate(predicate.xdr)
      .build()
  }

  /** Will return true if ledger closeTime < instant */
  case class AbsolutelyBefore(instant: Instant) extends ClaimPredicate {
    override def encode: LazyList[Byte] = Encode.int(4) ++ Encode.long(instant.getEpochSecond)
    override def check(claimCreation: Instant, instant: Instant): Boolean = !instant.isAfter(this.instant)

    override def xdr: XClaimPredicate = new XClaimPredicate.Builder()
      .discriminant(ClaimPredicateType.CLAIM_PREDICATE_BEFORE_ABSOLUTE_TIME)
      .absBefore(new Int64(instant.getEpochSecond))
      .build()
  }

  /** Seconds since closeTime of the ledger in which the ClaimableBalanceEntry was created */
  case class SinceClaimCreation(seconds: Long) extends ClaimPredicate {
    require(seconds >= 0, "Seconds must be non-negative")
    override def encode: LazyList[Byte] = Encode.int(5) ++ Encode.long(seconds)
    override def check(claimCreation: Instant, instant: Instant): Boolean =
      instant.minusSeconds(seconds).isBefore(claimCreation)

    override def xdr: XClaimPredicate = new XClaimPredicate.Builder()
      .discriminant(ClaimPredicateType.CLAIM_PREDICATE_BEFORE_RELATIVE_TIME)
      .relBefore(new Int64(seconds))
      .build()
  }

  private def parseClaimPredicate(o: Map[String, Any]): ClaimPredicate = o.headOption match {
    case Some(("unconditional", _)) => Unconditional
    case Some(("abs_before", time: String)) => AbsolutelyBefore(ZonedDateTime.parse(time).toInstant)
    case Some(("rel_before", seconds: String)) => SinceClaimCreation(seconds.toLong)
    case Some(("and", List(l, r))) => And(parseClaimPredicate(l.asInstanceOf[Map[String, Any]]), parseClaimPredicate(r.asInstanceOf[Map[String, Any]]))
    case Some(("or", List(l, r))) => Or(parseClaimPredicate(l.asInstanceOf[Map[String, Any]]), parseClaimPredicate(r.asInstanceOf[Map[String, Any]]))
    case Some(("not", p)) => Not(parseClaimPredicate(p.asInstanceOf[Map[String, Any]]))
  }

  def parseClaimPredicate(o: JObject): ClaimPredicate = parseClaimPredicate(o.values)

  def decodeXdr(xdr: XClaimPredicate): ClaimPredicate =
    xdr.getDiscriminant match {
      case ClaimPredicateType.CLAIM_PREDICATE_UNCONDITIONAL =>
        Unconditional
      case ClaimPredicateType.CLAIM_PREDICATE_AND =>
        And(decodeXdr(xdr.getAndPredicates().head), decodeXdr(xdr.getAndPredicates()(1)))
      case ClaimPredicateType.CLAIM_PREDICATE_NOT =>
        Not(decodeXdr(xdr.getNotPredicate))
      case ClaimPredicateType.CLAIM_PREDICATE_OR =>
        Or(decodeXdr(xdr.getOrPredicates().head), decodeXdr(xdr.getOrPredicates()(1)))
      case ClaimPredicateType.CLAIM_PREDICATE_BEFORE_ABSOLUTE_TIME =>
        AbsolutelyBefore(Instant.ofEpochSecond(xdr.getAbsBefore.getInt64))
      case ClaimPredicateType.CLAIM_PREDICATE_BEFORE_RELATIVE_TIME =>
        SinceClaimCreation(xdr.getRelBefore.getInt64)
    }
}

object ClaimPredicateDeserializer extends ResponseParser[ClaimPredicate](parseClaimPredicate)

package stellar.sdk.model

import java.time.{Instant, ZoneId}

import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.ClaimPredicate._
import stellar.sdk.model.ClaimPredicateGenerators.{genClaimPredicate, genInstant}

class ClaimPredicateSpec extends Specification with ScalaCheck {

  implicit val arbClaimPredicate: Arbitrary[ClaimPredicate] = Arbitrary(Gen.lzy(genClaimPredicate))
  implicit val arbInstant: Arbitrary[Instant] = Arbitrary(genInstant)
  implicit val formats = DefaultFormats + ClaimPredicateDeserializer

  "a claim predicate" should {
    "serde to/from XDR" >> prop { p: ClaimPredicate =>
      val (state, decoded) = ClaimPredicate.decode.run(p.encode).value
      state.isEmpty must beTrue
      decoded mustEqual p
    }

    "parse from JSON" >> prop { p: ClaimPredicate =>
      JsonMethods.parse(ClaimPredicateGenerators.json(p)).extract[ClaimPredicate] mustEqual p
    }
  }

  "Unconditional" should {
    "always be true" >> prop { (claimCreate: Instant, i: Instant) =>
      Unconditional.check(claimCreate, i) must beTrue
    }
  }

  "AbsolutelyBefore" should {
    "be true only if the given instant is before the declared instant" >> prop { (claimCreate: Instant, a: Instant, b: Instant) =>
      if (a == b) {
        AbsolutelyBefore(a).check(claimCreate, b) must beTrue
        AbsolutelyBefore(b).check(claimCreate, a) must beTrue
      } else if (a.isBefore(b)) {
        AbsolutelyBefore(a).check(claimCreate, b) must beFalse
        AbsolutelyBefore(b).check(claimCreate, a) must beTrue
      } else {
        AbsolutelyBefore(a).check(claimCreate, b) must beTrue
        AbsolutelyBefore(b).check(claimCreate, a) must beFalse
      }
    }
  }

  "SinceClaimCreation" should {
    "cannot be created with negative values" >> prop { seconds: Long =>
      SinceClaimCreation(seconds) must throwAn[IllegalArgumentException]
    }.setGen(Gen.negNum[Long])

    "be true only if the given instant is before claim creation plus the defined seconds" >> prop { (claimCreate: Instant, seconds: Long, i: Instant) =>
      val instantIsWithinTimeout = i.getEpochSecond - claimCreate.getEpochSecond < seconds
      SinceClaimCreation(seconds).check(claimCreate, i) mustEqual instantIsWithinTimeout
    }.setGen2(Gen.posNum[Long])
  }

  "And" should {
    "and two predicates" >> prop { (l: ClaimPredicate, r: ClaimPredicate, claimCreate: Instant, i: Instant) =>
      And(l, r).check(claimCreate, i) mustEqual
        l.check(claimCreate, i) && r.check(claimCreate, i)
    }
  }

  "Or" should {
    "or two predicates" >> prop { (l: ClaimPredicate, r: ClaimPredicate, claimCreate: Instant, i: Instant) =>
      Or(l, r).check(claimCreate, i) mustEqual
        l.check(claimCreate, i) || r.check(claimCreate, i)
    }
  }

  "Not" should {
    "invert a predicate" >> prop { (p: ClaimPredicate, claimCreate: Instant, i: Instant) =>
      Not(p).check(claimCreate, i) mustNotEqual p.check(claimCreate, i)
    }
  }
}

object ClaimPredicateGenerators extends ArbitraryInput {

  def genClaimPredicate: Gen[ClaimPredicate] = Gen.oneOf(
    Gen.const(Unconditional),
    Gen.lzy(genClaimPredicate.flatMap(l => genClaimPredicate.map(r => And(l, r)))),
    Gen.lzy(genClaimPredicate.flatMap(l => genClaimPredicate.map(r => Or(l, r)))),
    Gen.lzy(genClaimPredicate.map(Not)),
    genInstant.map(AbsolutelyBefore),
    Gen.posNum[Long].map(SinceClaimCreation)
  )

  def json(predicate: ClaimPredicate): String = predicate match {
    case Unconditional => """{"unconditional": true}"""
    case And(l, r) => s"""{"and":[${json(l)},${json(r)}]}"""
    case Or(l, r) => s"""{"or":[${json(l)},${json(r)}]}"""
    case Not(p) => s"""{"not":${json(p)}}"""
    case AbsolutelyBefore(i) => s"""{"abs_before":"${i.atZone(ZoneId.of("Z"))}"}"""
    case SinceClaimCreation(s) => s"""{"rel_before":"$s"}"""
  }
}
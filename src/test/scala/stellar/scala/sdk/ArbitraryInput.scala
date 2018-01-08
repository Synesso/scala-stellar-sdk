package stellar.scala.sdk

import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck

import scala.util.Random

trait ArbitraryInput extends ScalaCheck {

  implicit def arbKeyPair: Arbitrary[KeyPair] = Arbitrary(genKeyPair)

  implicit def arbVerifyingKey: Arbitrary[VerifyingKey] = Arbitrary(genVerifyingKey)

  implicit def arbAccount: Arbitrary[Account] = Arbitrary(genAccount)

  implicit def arbAmount: Arbitrary[Amount] = Arbitrary(genAmount)

  def genKeyPair: Gen[KeyPair] = Gen.oneOf(Seq(KeyPair.random))

  def genVerifyingKey: Gen[VerifyingKey] = genKeyPair.map(kp => VerifyingKey(kp.pk))

  def genAccount: Gen[Account] = for {
    kp <- genKeyPair
    seq <- Gen.posNum[Long]
  } yield {
    Account(kp, seq)
  }

  def genAmount: Gen[Amount] = Gen.posNum[Long].map(Amount.apply)

  def genCode(min: Int, max: Int): Gen[String] = Gen.choose(min, max).map(i => Random.alphanumeric.take(i).mkString)

}

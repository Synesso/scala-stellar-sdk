package stellar.scala.sdk

import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck

trait ArbitraryInput extends ScalaCheck {

  implicit def arbKeyPair: Arbitrary[KeyPair] = Arbitrary(genKeyPair)
  implicit def arbAccount: Arbitrary[Account] = Arbitrary(genAccount)

  def genKeyPair: Gen[KeyPair] = Gen.oneOf(Seq(KeyPair.random))

  def genAccount: Gen[Account] = for {
    kp <- genKeyPair
    seq <- Gen.posNum[Long]
  } yield Account(kp, seq)

}

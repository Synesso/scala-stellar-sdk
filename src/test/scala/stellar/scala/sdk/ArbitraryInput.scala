package stellar.scala.sdk

import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck

trait ArbitraryInput extends ScalaCheck {

  implicit def arbKeyPair: Arbitrary[KeyPair] = Arbitrary(genKeyPair)

  def genKeyPair: Gen[KeyPair] = Gen.oneOf(Seq(KeyPair.random))

}

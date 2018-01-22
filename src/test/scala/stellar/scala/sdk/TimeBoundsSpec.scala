package stellar.scala.sdk

import java.time.Instant

import org.specs2.mutable.Specification

import scala.util.Try

class TimeBoundsSpec extends Specification with ArbitraryInput {

  "time bounds creation" should {
    "fail if it doesn't ends after it begins" >> prop { (a: Instant, b: Instant) =>
      (if (a.isBefore(b)) TimeBounds(b, a)
      else TimeBounds(a, b)) must throwAn[AssertionError]
    }

    "succeed if it ends after it begins" >> prop { (a: Instant, b: Instant) => {
      Try {
        if (a.isBefore(b)) TimeBounds(a, b)
        else TimeBounds(b, a)
      } must beSuccessfulTry[TimeBounds]}.unless(a == b)
    }
  }
}

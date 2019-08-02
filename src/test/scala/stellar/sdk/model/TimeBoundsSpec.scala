package stellar.sdk.model

import java.time.Instant

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}
import scala.concurrent.duration._

import scala.util.Try

class TimeBoundsSpec extends Specification with ArbitraryInput with DomainMatchers {

  "time bounds creation" should {
    "fail if it doesn't ends after it begins" >> prop { (a: Instant, b: Instant) =>
      (if (a.isBefore(b)) {
        TimeBounds(b, a)
      } else {
        TimeBounds(a, b)
      }) must throwAn[IllegalArgumentException]
    }

    "succeed if it ends after it begins" >> prop { (a: Instant, b: Instant) => {
      Try {
        if (a.isBefore(b)) TimeBounds(a, b) else TimeBounds(b, a)
      } must beSuccessfulTry[TimeBounds]
    }.unless(a == b)
    }

    "be via a 'from-now' timeout" >> {
      val now = Instant.now().toEpochMilli
      val tb = TimeBounds.timeout(1.minute)
      tb.start.toEpochMilli must beCloseTo(now, delta = 100)
      tb.end.toEpochMilli must beCloseTo(now + 60000, delta = 100)
    }
  }

  "time bounds" should {
    "serde to/from xdr" >> prop { tb: TimeBounds =>
      tb must serdeUsing(TimeBounds.decode)
    }
  }
}

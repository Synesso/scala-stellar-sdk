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
      tb.start.toEpochMilli must beCloseTo(now - 5000, delta = 1000)
      tb.end.toEpochMilli must beCloseTo(now + 60000, delta = 1000)
    }
  }

  "time bounds" should {
    "serde to/from xdr" >> prop { tb: TimeBounds =>
      tb must serdeUsing(TimeBounds.decode)
    }

    "exclude any instant before the start" >> prop { tb: TimeBounds =>
      tb.includes(tb.start.minusNanos(1)) must beFalse
    }

    "exclude any instant after the end" >> prop { tb: TimeBounds =>
      tb.includes(tb.end.plusNanos(1)) must beFalse
    }

    "include the instant at the start" >> prop { tb: TimeBounds =>
      tb.includes(tb.start) must beTrue
    }

    "include the instant at the end" >> prop { tb: TimeBounds =>
      tb.includes(tb.end) must beTrue
    }

    "include the instants within the bounds" >> prop { tb: TimeBounds =>
      val midpoint = Instant.ofEpochMilli((tb.end.toEpochMilli - tb.start.toEpochMilli) / 2 + tb.start.toEpochMilli)
      tb.includes(midpoint) must beTrue
    }
  }

  "unbounded time bounds" should {
    "always include any instant" >> prop { instant: Instant =>
      TimeBounds.Unbounded.includes(instant) must beTrue
    }
  }
}

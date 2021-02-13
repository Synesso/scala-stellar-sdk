package stellar.sdk.model

import java.time.temporal.ChronoField
import java.time.{Clock, Instant}

import org.stellar.xdr.{TimePoint, Uint64, TimeBounds => XTimeBounds}

import scala.concurrent.duration.Duration

case class TimeBounds(start: Instant, end: Instant) {
  def xdr: XTimeBounds = new XTimeBounds.Builder()
    .minTime(new TimePoint(new Uint64(start.getEpochSecond)))
    .maxTime(new TimePoint(new Uint64(end.getEpochSecond)))
    .build()

  private val isUnbounded: Boolean = start == end && start.getEpochSecond == 0
  require(start.isBefore(end) || isUnbounded, s"Range start is not before the end [start=$start][end=$end]")

  /**
   * Whether the given instant is within these bounds, inclusive.
   */
  def includes(instant: Instant): Boolean =
    isUnbounded || !(start.isAfter(instant) || end.isBefore(instant))
}

object TimeBounds {
  def decodeXdr(xdr: XTimeBounds): TimeBounds = if (xdr == null) Unbounded else TimeBounds(
    start = Instant.ofEpochSecond(xdr.getMinTime.getTimePoint.getUint64),
    end = Instant.ofEpochSecond(xdr.getMaxTime.getTimePoint.getUint64)
  )

  val Unbounded: TimeBounds = TimeBounds(Instant.ofEpochSecond(0), Instant.ofEpochSecond(0))

  def timeout(duration: Duration, clock: Clock = Clock.systemUTC()): TimeBounds = {
    val now = clock.instant().`with`(ChronoField.NANO_OF_SECOND, 0)
    TimeBounds(now.minusSeconds(5), now.plusMillis(duration.toMillis))
  }
}

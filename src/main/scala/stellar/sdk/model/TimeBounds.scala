package stellar.sdk.model

import java.time.{Clock, Instant}
import java.time.temporal.{ChronoField, TemporalAdjuster, TemporalField}

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable}

import scala.concurrent.duration.Duration

case class TimeBounds(start: Instant, end: Instant) extends Encodable {
  private val isUnbounded: Boolean = start == end && start.getEpochSecond == 0
  require(start.isBefore(end) || isUnbounded, s"Range start is not before the end [start=$start][end=$end]")

  /**
   * Whether the given instant is within these bounds, inclusive.
   */
  def includes(instant: Instant): Boolean =
    isUnbounded || !(start.isAfter(instant) || end.isBefore(instant))

  def encode: LazyList[Byte] = {
    import stellar.sdk.model.xdr.Encode._

    instant(start) ++ instant(end)
  }
}

object TimeBounds extends Decode {
  def decode: State[Seq[Byte], TimeBounds] = for {
    start <- instant
    end <- instant
  } yield TimeBounds(start, end)

  val Unbounded: TimeBounds = TimeBounds(Instant.ofEpochSecond(0), Instant.ofEpochSecond(0))

  def timeout(duration: Duration, clock: Clock = Clock.systemUTC()): TimeBounds = {
    val now = clock.instant().`with`(ChronoField.NANO_OF_SECOND, 0)
    TimeBounds(now.minusSeconds(5), now.plusMillis(duration.toMillis))
  }
}

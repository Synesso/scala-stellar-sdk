package stellar.sdk.model

import java.time.Instant
import java.time.temporal.{ChronoField, TemporalAdjuster, TemporalField}

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable}

import scala.concurrent.duration.Duration

case class TimeBounds(start: Instant, end: Instant) extends Encodable {
  require(start.isBefore(end) || (start == end && start.getEpochSecond == 0),
    s"Range start is not before the end [start=$start][end=$end]")

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

  val Unbounded = TimeBounds(Instant.ofEpochSecond(0), Instant.ofEpochSecond(0))

  def timeout(duration: Duration) = {
    val now = Instant.now().`with`(ChronoField.NANO_OF_SECOND, 0)
    TimeBounds(now.minusSeconds(5), now.plusMillis(duration.toMillis))
  }

}

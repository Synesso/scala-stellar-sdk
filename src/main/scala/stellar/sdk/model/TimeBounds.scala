package stellar.sdk.model

import java.time.Instant

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable}

import scala.concurrent.duration.Duration

case class TimeBounds(start: Instant, end: Instant) extends Encodable {
  require(start.isBefore(end))

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

  val Unbounded = TimeBounds(Instant.MIN, Instant.MAX)

  def timeout(duration: Duration) = {
    val now = Instant.now()
    TimeBounds(now.minusSeconds(5), now.plusMillis(duration.toMillis))
  }

}

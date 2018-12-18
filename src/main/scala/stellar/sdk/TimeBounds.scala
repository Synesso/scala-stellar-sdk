package stellar.sdk

import java.time.Instant

import org.stellar.sdk.xdr.{TimeBounds => XDRTimeBounds}
import stellar.sdk.XDRPrimitives._

case class TimeBounds(start: Instant, end: Instant) extends Encodable {
  assert(start.isBefore(end))

  def toXDR: XDRTimeBounds = {
    val tb = new XDRTimeBounds
    tb.setMinTime(uint64(start.toEpochMilli))
    tb.setMaxTime(uint64(end.toEpochMilli))
    tb
  }

  override def encode: Stream[Byte] = Encode.long(start.toEpochMilli) ++ Encode.long(end.toEpochMilli)
}

object TimeBounds {

  def fromXDR(timeBounds: XDRTimeBounds): TimeBounds = {
    TimeBounds(
      start = Instant.ofEpochMilli(timeBounds.getMinTime.getUint64),
      end = Instant.ofEpochMilli(timeBounds.getMaxTime.getUint64)
    )
  }
}

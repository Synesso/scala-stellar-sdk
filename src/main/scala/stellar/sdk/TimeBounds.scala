package stellar.sdk

import java.time.Instant

import org.stellar.sdk.xdr.{TimeBounds => XDRTimeBounds}

case class TimeBounds(start: Instant, end: Instant) extends XDRPrimitives {
  assert(start.isBefore(end))

  def toXDR: XDRTimeBounds = {
    val tb = new XDRTimeBounds
    tb.setMinTime(uint64(start.toEpochMilli))
    tb.setMaxTime(uint64(end.toEpochMilli))
    tb
  }
}

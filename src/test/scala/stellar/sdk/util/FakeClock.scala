package stellar.sdk.util

import java.time.{Clock, Instant, ZoneId, ZonedDateTime}

import scala.concurrent.duration._

case class FakeClock(
  zoneId: ZoneId = ZoneId.of("UTC")
) extends Clock {

  private var fixedInstant: Instant = ZonedDateTime.of(2020, 8, 15, 0, 0, 0, 0, zoneId).toInstant

  override def getZone: ZoneId = zoneId

  override def withZone(zoneId: ZoneId): Clock = this.copy(zoneId = zoneId)

  override def instant(): Instant = fixedInstant

  def advance(duration: Duration): Unit = fixedInstant = fixedInstant.plus(java.time.Duration.ofNanos(duration.toNanos))
}

package stellar.sdk.model

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

case class TradeAggregation() {

}

object TradeAggregation {

  sealed class Resolution(val duration: Duration)

  val OneMinute = new Resolution(Duration.create(1, TimeUnit.MINUTES))
  val FiveMinutes = new Resolution(OneMinute.duration * 5.0)
  val FifteenMinutes = new Resolution(FiveMinutes.duration * 3.0)
  val OneHour = new Resolution(FifteenMinutes.duration * 4.0)
  val OneDay = new Resolution(OneHour.duration * 24.0)
  val OneWeek = new Resolution(OneDay.duration * 7.0)

}

package stellar.sdk.model

import java.time.Instant
import java.util.concurrent.TimeUnit

import org.json4s.JsonAST.JObject
import org.json4s.{DefaultFormats, JValue}
import stellar.sdk.model.response.ResponseParser

import scala.concurrent.duration.Duration

case class TradeAggregation(instant: Instant, tradeCount: Int, baseVolume: Double, counterVolume: Double,
                            average: Double, open: Price, high: Price, low: Price, close: Price)

object TradeAggregationDeserializer extends ResponseParser[TradeAggregation]({ o: JObject =>
  implicit val formats = DefaultFormats

  def price(p: JValue): Price = Price((p \ "N").extract[Int], (p \ "D").extract[Int])

  TradeAggregation(
    instant = Instant.ofEpochMilli((o \ "timestamp").extract[String].toLong),
    tradeCount = (o \ "trade_count").extract[String].toInt,
    baseVolume = (o \ "base_volume").extract[String].toDouble,
    counterVolume = (o \ "counter_volume").extract[String].toDouble,
    average = (o \ "avg").extract[String].toDouble,
    open = price(o \ "open_r"),
    high = price(o \ "high_r"),
    low = price(o \ "low_r"),
    close = price(o \ "close_r"))
})

object TradeAggregation {

  sealed class Resolution(val duration: Duration)

  val OneMinute = new Resolution(Duration.create(1, TimeUnit.MINUTES))
  val FiveMinutes = new Resolution(OneMinute.duration * 5.0)
  val FifteenMinutes = new Resolution(FiveMinutes.duration * 3.0)
  val OneHour = new Resolution(FifteenMinutes.duration * 4.0)
  val OneDay = new Resolution(OneHour.duration * 24.0)
  val OneWeek = new Resolution(OneDay.duration * 7.0)

}

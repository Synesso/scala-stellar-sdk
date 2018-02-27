package stellar.sdk

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats, JValue}

case class OrderBook(selling: Asset, buying: Asset, bids: Seq[Order], asks: Seq[Order]) {
  def base = selling
  def counter = buying
}

case class Order(price: Price, quantity: Long)

object OrderBookDeserializer extends CustomSerializer[OrderBook](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats

    def asset(j: JValue) = ???
    def orders(j: JValue) = ???
    OrderBook(
      selling = asset(o \ "base"),
      buying = asset(o \ "counter"),
      bids = orders(o \ "bids"),
      asks = orders(o \ "asks")
    )
}, PartialFunction.empty)
)

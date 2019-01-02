package stellar.sdk.model.response

import org.json4s.NoTypeHints
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.specs2.mutable.Specification
import stellar.sdk._
import stellar.sdk.model.{Amount, Order, OrderBook, OrderBookDeserializer}
import stellar.sdk.model.op.JsonSnippets

class OrderBookSpec extends Specification with ArbitraryInput with JsonSnippets {

  implicit val formats = Serialization.formats(NoTypeHints) + OrderBookDeserializer

  "order book" should {
    "parse from json" >> prop { ob: OrderBook =>
      val doc =
        s"""
           |{
           |  "bids": [${ob.bids.map(order).mkString(",")}],
           |  "asks": [${ob.asks.map(order).mkString(",")}],
           |  "base": {${asset(ob.selling)}}
           |  "counter": {${asset(ob.buying)}}
           |}
        """.stripMargin

      parse(doc).extract[OrderBook] mustEqual ob
    }
  }

  private def order(o: Order) =
    s"""{
       |  "price_r": {
       |    "n": ${o.price.n},
       |    "d": ${o.price.d}
       |  },
       |  "price": "${o.price.asDecimalString}",
       |  "amount": "${Amount.toDisplayUnits(o.quantity)}"
       |}
     """.stripMargin

}

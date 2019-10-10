package stellar.sdk.model

import java.util.Locale

import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, JValue}
import stellar.sdk.KeyPair
import stellar.sdk.model.response.ResponseParser

case class OrderBook(selling: Asset, buying: Asset, bids: Seq[Order], asks: Seq[Order])

case class Order(price: Price, quantity: Long) {
  def toDisplayUnits: String = "%.7f".formatLocal(Locale.ROOT, quantityBigDecimal)

  def quantityBigDecimal: BigDecimal = BigDecimal(quantity) / Amount.toIntegralFactor

  override def toString: String = s"Order(price=${price.asDecimalString}, quantity=$toDisplayUnits)"

  def take(qty: Long): Option[Order] = Some(this.copy(quantity = quantity - qty))
    .filter(_.quantity > 0)
}

object OrderBookDeserializer extends ResponseParser[OrderBook]({ o: JObject =>
  implicit val formats = DefaultFormats

  def asset(obj: JValue) = {
    def assetCode = (obj \ s"asset_code").extract[String]

    def assetIssuer = KeyPair.fromAccountId((obj \ s"asset_issuer").extract[String])

    (obj \ s"asset_type").extract[String] match {
      case "native" => NativeAsset
      case "credit_alphanum4" => IssuedAsset4(assetCode, assetIssuer)
      case "credit_alphanum12" => IssuedAsset12(assetCode, assetIssuer)
      case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
    }
  }

  def orders(obj: JValue) = {
    obj.children.map(c =>
      Order(
        price = Price(
          n = (c \ "price_r" \ "n").extract[Int],
          d = (c \ "price_r" \ "d").extract[Int]
        ),
        quantity = Amount.toBaseUnits((c \ "amount").extract[String]).get
      ))
  }

  try {
    OrderBook(
      selling = asset(o \ "base"),
      buying = asset(o \ "counter"),
      bids = orders(o \ "bids"),
      asks = orders(o \ "asks")
    )
  } catch {
    case t: Throwable => throw new RuntimeException(pretty(render(o)), t)
  }
})

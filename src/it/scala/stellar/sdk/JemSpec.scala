package stellar.sdk

import org.specs2.mutable.Specification
import stellar.sdk.Jem.overlap
import stellar.sdk.model.op.{PathPaymentStrictReceiveOperation, PayOperation}
import stellar.sdk.model.{Amount, Asset, NativeAmount, NativeAsset, Order, Price}

class JemSpec extends Specification {

  val accn = KeyPair.random
  val from = Asset("FROM", KeyPair.random)
  val to = Asset("TO", KeyPair.random)
  val xlm = NativeAsset

  val amount15 = NativeAmount(15)
  val amount1000 = NativeAmount(1000)

  def order(qty: Int) = new {
    def at(cents: Int) = Order(Price(cents, 100), qty)
  }

  "overlap function" should {
    "be nothing when there are no orders" >> {
      overlap(Nil, Nil, amount1000, accn, from, to) must beNone
    }
    "be nothing when there are no bids" >> {
      overlap(
        bids = Nil,
        asks = List(order(50).at(90)), amount1000, accn, from, to) must beNone
    }
    "be nothing when there's no asks" >> {
      overlap(
        bids = List(order(50).at(90)),
        asks = Nil, amount1000, accn, from, to) must beNone
    }
    "be nothing when there's no overlap" >> {
      overlap(
        bids = List(order(50).at(90)),
        asks = List(order(50).at(95)), amount1000, accn, from, to) must beNone
    }
    "match a single order overlap" >> {
      overlap(
        bids = List(order(50).at(95)),
        asks = List(order(50).at(90)), amount1000, accn, from, to) must beSome(
        PathPaymentStrictReceiveOperation(
          sendMax = Amount(50, from),
          destinationAccount = accn.asPublicKey,
          destinationAmount = Amount(50, to),
          path = List(xlm),
          sourceAccount = Some(accn.asPublicKey)))
    }
    "match a bid partially overlapping an ask" >> {
      overlap(
        bids = List(order(30).at(95)),
        asks = List(order(50).at(90)), amount1000, accn, from, to) must beSome(
        PathPaymentStrictReceiveOperation(
          sendMax = Amount(30, from),
          destinationAccount = accn.asPublicKey,
          destinationAmount = Amount(30, to),
          path = List(xlm),
          sourceAccount = Some(accn.asPublicKey)))
    }
    "match an ask partially overlapping a bid" >> {
      overlap(
        bids = List(order(30).at(95)),
        asks = List(order(20).at(90)), amount1000, accn, from, to) must beSome(
        PathPaymentStrictReceiveOperation(
          sendMax = Amount(20, from),
          destinationAccount = accn.asPublicKey,
          destinationAmount = Amount(20, to),
          path = List(xlm),
          sourceAccount = Some(accn.asPublicKey)))
    }
    "match a bid overlapping ask to max limit" >> {
      overlap(
        bids = List(order(30).at(95)),
        asks = List(order(50).at(90)), amount15, accn, from, to) must beSome(
        PathPaymentStrictReceiveOperation(
          sendMax = Amount(15, from),
          destinationAccount = accn.asPublicKey,
          destinationAmount = Amount(15, to),
          path = List(xlm),
          sourceAccount = Some(accn.asPublicKey)))
    }
    "match 2.5 bids partially overlapping an ask" >> {
      overlap(
        bids = List(order(5).at(96), order(8).at(97), order(13).at(98)),
        asks = List(order(50).at(90)), amount1000, accn, from, to) must beSome(
        PathPaymentStrictReceiveOperation(
          sendMax = Amount(26, from),
          destinationAccount = accn.asPublicKey,
          destinationAmount = Amount(26, to),
          path = List(xlm),
          sourceAccount = Some(accn.asPublicKey)))
    }
    "match a bid overlapping 2.5 asks" >> {
      overlap(
        bids = List(order(30).at(95)),
        asks = List(order(13).at(90), order(14).at(92), order(7).at(94)), amount1000, accn, from, to) must beSome(
        PathPaymentStrictReceiveOperation(
          sendMax = Amount(30, from),
          destinationAccount = accn.asPublicKey,
          destinationAmount = Amount(30, to),
          path = List(xlm),
          sourceAccount = Some(accn.asPublicKey)))
    }
  }

}

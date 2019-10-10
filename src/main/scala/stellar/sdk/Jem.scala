package stellar.sdk

import stellar.sdk.model.{Amount, Asset, Balance, NativeAsset, Order}

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Jem {


  def main(args: Array[String]): Unit = {

    val nao = Asset("BTC", KeyPair.fromAccountId("GATEMHCCKCY67ZUCKTROYN24ZYT5GK4EQZ65JJLDHKHRUZI3EUEKMTCH"))
    val apay = Asset("BTC", KeyPair.fromAccountId("GAUTUYY2THLF7SGITDFMXJVYH3LHDSMGEAKSBU267M2K7A3W543CKUEF"))
    val xlm = NativeAsset
    val me = KeyPair.fromAccountId("GAZ5TP7VHVHFJE6EXF5UAN3P5IYFVSA2RLC3573YQCCWWWBMWB4FLUXE")


    @tailrec
    def loop(next: Long = 0L): Unit = {
      val now = System.currentTimeMillis()
      if (now > next) {
        val naoBookF = PublicNetwork.orderBook(nao, xlm)
        val apayBookF = PublicNetwork.orderBook(apay, xlm)
        val balanceF = PublicNetwork.account(me).map(_.balances.find(_.amount.asset == nao))
        val o = Await.result(
          for {
            balance <- balanceF
            apayBook <- apayBookF
            naoBook <- naoBookF
          } yield {
            //        println("BALANCE")
            //        println(balance)

            //        println("NAO BIDS")
            //        naoBook.bids.sortBy(_.price.asBigDecimal).reverse.foreach(println)

            //        println("APAY ASKS")
            //        apayBook.asks.sortBy(_.price.asBigDecimal).foreach(println)

            balance.flatMap(b =>
              overlap(
                naoBook.bids.sortBy(_.price.asBigDecimal).reverse,
                apayBook.asks.sortBy(_.price.asBigDecimal),
                maxSell = b.amount
              )
            )
          }, 10.seconds)
        println(s"OVERLAP=$o")
        loop(now + 5000)
      } else {
        Thread.sleep(250)
        loop(next)
      }
    }

    loop()

  }

  def overlap(bids: Seq[Order], asks: Seq[Order], maxSell: Amount): Option[(Order, Order)] = {
    @scala.annotation.tailrec
    def loop(bidsTail: Seq[Order], asksTail: Seq[Order], sellRemain: Amount, acc: Option[(Order, Order)]): Option[(Order, Order)] = {
      if (sellRemain.units == 0) acc
      else bidsTail match {
        case Nil => acc
        case bid +: bt => asksTail match {
          case Nil => acc
          case ask +: _ if ask.price >= bid.price => acc
          case ask +: at =>
            val matching = math.max(math.max(ask.quantity, bid.quantity), sellRemain.units)
            loop(
              bid.take(matching).map(_ +: bt).getOrElse(bt),
              ask.take(matching).map(_ +: at).getOrElse(at),
              sellRemain.minus(matching),
              acc.map { case (b, a) =>
                  bid.copy(quantity = bid.quantity + b.quantity) -> ask.copy(quantity = ask.quantity + a.quantity)
              }.orElse(Some(bid.copy(quantity = matching) -> ask.copy(quantity = matching)))
            )
        }
      }
    }
    loop(bids, asks, maxSell, None)
  }
}

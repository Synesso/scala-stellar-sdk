package stellar.sdk

import stellar.sdk.model.TimeBounds.timeout
import stellar.sdk.model.op.{PathPaymentStrictReceiveOperation, PathPaymentStrictSendOperation}
import stellar.sdk.model.{Amount, Asset, Balance, MemoText, NativeAmount, NativeAsset, Order, TimeBounds, Transaction}

import scala.annotation.tailrec
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Jem {

  implicit val network = PublicNetwork

  def main(args: Array[String]): Unit = {

    val Array(seed) = args
    val me = KeyPair.fromSecretSeed(seed)

    val nao = Asset("BTC", KeyPair.fromAccountId("GATEMHCCKCY67ZUCKTROYN24ZYT5GK4EQZ65JJLDHKHRUZI3EUEKMTCH"))
    val apay = Asset("BTC", KeyPair.fromAccountId("GAUTUYY2THLF7SGITDFMXJVYH3LHDSMGEAKSBU267M2K7A3W543CKUEF"))
    val xlm = NativeAsset

    @tailrec
    def loop(next: Long = 0L): Unit = {
      val now = System.currentTimeMillis()
      if (now > next) {
        val naoBookF = network.orderBook(nao, xlm)
        val apayBookF = network.orderBook(apay, xlm)
        val accountF = network.account(me)
        val balanceF = accountF.map(_.balances.find(_.amount.asset == nao))
        val p = Await.result(for {
          maybeBalance <- balanceF
          apayBook <- apayBookF
          naoBook <- naoBookF
          account <- accountF
          maybePayment <- Future(maybeBalance.flatMap(b =>
            overlap(
              naoBook.bids.sortBy(_.price.asBigDecimal).reverse,
              apayBook.asks.sortBy(_.price.asBigDecimal),
              maxSell = b.amount,
              me, nao, apay)))
        } yield {
          maybePayment.map { payment =>
            Transaction(account, List(payment), MemoText("DrAvocado was here"), timeout(30.seconds), NativeAmount(500000))
          }
        }, 10.seconds)
        println(s"PAYMENT=$p")
        loop(now + 5000)
      } else {
        Thread.sleep(250)
        loop(next)
      }
    }

    loop()

  }

  def overlap(bids: Seq[Order], asks: Seq[Order], maxSell: Amount, accn: KeyPair, from: Asset, to: Asset)
  : Option[PathPaymentStrictReceiveOperation] = {

    @scala.annotation.tailrec
    def loop(bidsTail: Seq[Order], asksTail: Seq[Order], sellRemain: Amount,
             acc: Option[PathPaymentStrictReceiveOperation]): Option[PathPaymentStrictReceiveOperation] = {

      if (sellRemain.units == 0) acc
      else bidsTail match {
        case Nil => acc
        case bid +: bt => asksTail match {
          case Nil => acc
          case ask +: _ if ask.price >= bid.price => acc
          case ask +: at =>
            val matching = math.min(math.min(ask.quantity, bid.quantity), sellRemain.units)
            loop(
              bid.take(matching).map(_ +: bt).getOrElse(bt),
              ask.take(matching).map(_ +: at).getOrElse(at),
              sellRemain.minus(matching),
              Some(acc match {
                case None =>
                  PathPaymentStrictReceiveOperation(
                    sendMax = Amount(matching, from),
                    destinationAccount = accn.asPublicKey,
                    destinationAmount = Amount(matching, to),
                    path = List(NativeAsset),
                    sourceAccount = Some(accn.asPublicKey))
                case Some(payment) =>
                  payment.copy(
                    sendMax = Amount(payment.sendMax.units + matching, from),
                    destinationAmount = Amount(payment.destinationAmount.units + matching, to),
                  )
              }))
        }
      }
    }

    loop(bids, asks, maxSell, None)
  }
}

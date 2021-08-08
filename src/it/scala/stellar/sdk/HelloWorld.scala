package stellar.sdk

import stellar.sdk.PublicNetwork
import stellar.sdk.model.{Asc, Desc, Now, Trade}

import java.time.ZonedDateTime
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}

object HelloWorld extends App {
  def traverseStream[A, B](in: LazyList[A])(fn: A => Future[B]): Future[LazyList[B]] = {
    in match {
      case LazyList.cons(head, tail) =>
        for {
          newHead <- fn(head)
          newTail <- traverseStream(tail)(fn)
        } yield newHead #:: newTail
      case _ =>
        Future.successful(LazyList.empty)
    }
  }

  // Works
  for {
    trades <- PublicNetwork.trades(Now, Asc)
  } yield {
    val result = traverseStream(trades) {
      case Trade(id, time, offerId, baseOfferId, counterOfferId, _, _, _, _, _) => {
        Future.successful(System.out.println(s"New trade coming in Trade($id, $time, $offerId)"))
      }
    }
    val stream = Await.result(result, Duration.Inf)
  }

  val timeWindow = ZonedDateTime.now().minusMinutes(65)
}
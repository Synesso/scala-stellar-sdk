package stellar.sdk

import cats.effect.{IO, Ref}
import cats.effect.unsafe.implicits.{global => catsGlobal}
import com.typesafe.scalalogging.LazyLogging
import stellar.sdk.model.op.{AccountMergeOperation, CreateAccountOperation}
import stellar.sdk.model._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TestAccounts(quantity: Int = 20) extends LazyLogging {
  require(quantity > 0 && quantity <= 20, "Quantity must be positive and no more than 20. (20 is a temporary limit)")

  implicit val network: TestNetwork.type = TestNetwork

  private val unused: Ref[IO, List[KeyPair]] = Ref[IO].of(List.fill(quantity) {
    KeyPair.random
  }).unsafeRunSync()
  private val borrowed: Ref[IO, List[KeyPair]] = Ref[IO].of(List.empty[KeyPair]).unsafeRunSync()
  private val friendBot: Ref[IO, Option[AccountId]] = Ref[IO].of(Option.empty[AccountId]).unsafeRunSync()

  /**
   * Creates all of the accounts from KeyPairs that were generated at the time of construction.
   */
  def open(): Unit = {
    logger.debug(s"Opening $quantity accounts")
    try {
      unused.get.map {
        case h +: t =>
          val friendBotResponse = network.fund(h)
          friendBotResponse.foreach { r => friendBot.set(Some(r.transaction.transaction.source.id)).unsafeRunSync() }
          val createOps = t.map(kp => CreateAccountOperation(
            destinationAccount = kp.toAccountId,
            startingBalance = NativeAmount(10_000_0000000L / quantity)
          ))
          createOps.map(_.toString).foreach(s => logger.debug(s))
          val response = for {
            _ <- friendBotResponse
            sourceAccount <- network.account(h)
            txn = Transaction(sourceAccount, createOps, NoMemo, TimeBounds.Unbounded, maxFee = Amount.lumens(1))
            response <- txn.sign(h).submit()
          } yield response
          response.onComplete {
            case Success(r) => logger.debug(r.toString)
            case Failure(t) => logger.error("Failed to open accounts", t)
          }
          Await.ready(response, 30.seconds)
      }.unsafeRunSync()
    } catch {
      case t: Throwable => logger.error("Failed to open accounts", t.printStackTrace())
    }

  }

  /**
   * Folds all of the accounts back into FriendBot.
   */
  def close(): Unit = {
    try {
      logger.debug(s"Closing $quantity accounts")
      val ops = for {
        friendBotAccountId <- friendBot.get.map(_.get)
        allKps <- unused.get.flatMap(kps => borrowed.get.map(_ ++ kps))
        allOps = allKps.map { kp => AccountMergeOperation(friendBotAccountId, Some(kp)) }
      } yield allKps.zip(allOps).toMap
      val opsMap = ops.unsafeRunSync()
      val response = for {
        account <- network.account(opsMap.keys.toList.head)
        txn = Transaction(account, opsMap.values.toList, NoMemo, TimeBounds.Unbounded, Amount.lumens(1))
        signedTxn = opsMap.keysIterator.foldLeft(SignedTransaction(txn, Nil)) { case (t, kp) => t.sign(kp) }
        response <- signedTxn.submit()
      } yield response
      response.onComplete {
        case Success(r) => logger.debug(r.toString)
        case Failure(t) => logger.error("Failed to close accounts", t.printStackTrace())
      }
      Await.result(response, 1.minute)
    } catch {
      case t: Throwable => logger.error("Failed to close accounts", t.printStackTrace())
    }

  }

  /**
   * Returns the next unused KeyPairs, or dies.
   */
  def take(i: Int): List[KeyPair] = {
    logger.debug(s"Taking $i accounts")
    val ioNext = for {
      next <- unused.modify { list => list.splitAt(list.size - i) }
      _ <- borrowed.update(next ++ _)
    } yield next
    ioNext.unsafeRunSync()
  }

  /**
   * Returns the next unused KeyPair, or dies.
   */
  def take: KeyPair = take(1).head
}

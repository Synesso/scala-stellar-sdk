package stellar.sdk.util

import org.json4s.CustomSerializer
import stellar.sdk.Network
import stellar.sdk.inet.HorizonAccess
import stellar.sdk.model.response.{DataValueResponse, TransactionPostResponse}
import stellar.sdk.model.{HorizonCursor, HorizonOrder, SignedTransaction}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class DoNothingNetwork(override val passphrase: String = "Scala SDK do-nothing network") extends Network {
  override val horizon: HorizonAccess = new HorizonAccess {
    override def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResponse] = ???

    override def get[T: ClassTag](path: String, params: Map[String, String])
      (implicit ec: ExecutionContext, m: Manifest[T]): Future[T] =
      if (path.endsWith("data/data_key")) {
        Future(DataValueResponse("00").asInstanceOf[T])(ec)
      } else ???

    override def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder, params: Map[String, String] = Map.empty)
      (implicit ec: ExecutionContext, m: Manifest[T]): Future[LazyList[T]] =
      ???

    override def getSeq[T: ClassTag](path: String, de: CustomSerializer[T], params: Map[String, String])
      (implicit ec: ExecutionContext, m: Manifest[T]): Future[LazyList[T]] =
      Future.successful(LazyList.empty[T])
  }
}

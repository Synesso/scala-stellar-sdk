package stellar.sdk.inet

import org.json4s.CustomSerializer
import stellar.sdk.model._
import stellar.sdk.model.response._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag


trait HorizonAccess {

  def post(txn: SignedTransaction)(implicit ec: ExecutionContext): Future[TransactionPostResponse]

  def get[T: ClassTag](path: String, params: Map[String, String] = Map.empty)
                      (implicit ec: ExecutionContext, m: Manifest[T]): Future[T]

  def getStream[T: ClassTag](path: String, de: CustomSerializer[T], cursor: HorizonCursor, order: HorizonOrder, params: Map[String, String] = Map.empty)
                            (implicit ec: ExecutionContext, m: Manifest[T]): Future[Stream[T]]

  def getSeq[T: ClassTag](path: String, de: CustomSerializer[T], params: Map[String, String] = Map.empty)
                            (implicit ec: ExecutionContext, m: Manifest[T]): Future[Seq[T]]

}

case class RestException(message: String, t: Throwable = None.orNull) extends Exception(message, t)


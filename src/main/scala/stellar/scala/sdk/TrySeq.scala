package stellar.scala.sdk

import scala.util.{Failure, Success, Try}

trait TrySeq {
  def sequence[T](tries: Seq[Try[T]]): Try[Seq[T]] = {
    tries.foldLeft(Success(Seq.empty[T]): Try[Seq[T]]) {
      case (Success(acc), Success(t)) => Success(t +: acc)
      case (Success(_), Failure(t)) => Failure(t)
      case (failure, _) => failure
    }.map(_.reverse)
  }
}

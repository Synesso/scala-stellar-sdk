package stellar.scala.sdk.inet

import java.net.URI

import stellar.scala.sdk.SignedTransaction
import stellar.scala.sdk.resp.SubmitTransactionResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scalaj.http.Http

case class Server(uri: URI) {
  def post(txn: SignedTransaction): Future[SubmitTransactionResponse] = {
    for {
      tx <- Future.fromTry(txn.toEnvelopeXDRBase64)
      req = Http(uri.toString).postForm(Seq("tx" -> tx.mkString))
      response = req.asString
      _ = println(s"raw response = $response")
    } yield SubmitTransactionResponse()
  }

}

object Server {
  def apply(uri: String): Try[Server] = Try {
    Server(URI.create(uri))
  }
}

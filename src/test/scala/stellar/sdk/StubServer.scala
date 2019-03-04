package stellar.sdk

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class StubServer(port: Int = 8002)(implicit ec: ExecutionContext) extends Directives {

  implicit val system = ActorSystem("stub-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  //                              path        params               response
  @volatile var state = Map.empty[String, Map[Map[String, String], Reply]]

  private def updateState(pathString: String, params: Map[String, String], response: Reply): Unit = this.synchronized {
    state = {
      state.updated(pathString, state.getOrElse(pathString, Map.empty).updated(params, response))
    }
  }

  def expectGet(pathString: String, params: Map[String, String], response: String): Unit = {
    updateState(pathString, params, ReplyWithJson(response))
  }

  def forgetGet(pathString: String, params: Map[String, String]): Unit = {
    updateState(pathString, params, ReplyWithNothing)
  }

  def expectGetRedirect(base: String, pathString: String, viaPath: String, params: Map[String, String], response: String): Unit = {
    expectGet(pathString, params, response)
    updateState(viaPath, params, ReplyWithRedirect(s"$base/$pathString"))
  }

  val route: Route = ctx => {
    val routes = state.map { case (pathString, responses) =>
      get {
        path(pathString) {
          parameterMap { params =>
            responses.get(params).map {
              case ReplyWithNothing => complete(HttpResponse(404, entity = "nothing here"))
              case ReplyWithJson(response) =>
                complete(HttpResponse(entity = response).mapEntity(_.withContentType(`application/json`)))
              case ReplyWithRedirect(to) => redirect(to, StatusCodes.PermanentRedirect)
            }.getOrElse(
              complete(HttpResponse(404, entity = "No such expectation"))
            )
          }
        }
      }
    }
    concat(routes.toList: _*)(ctx)
  }

  private val binding: Future[ServerBinding] = {
    Http().bindAndHandle(route, "0.0.0.0", port)
  }

  def stop(): Unit = binding.map(_.terminate(1.minute))

  sealed trait Reply
  case object ReplyWithNothing extends Reply
  case class ReplyWithJson(s: String) extends Reply
  case class ReplyWithRedirect(to: String) extends Reply
}

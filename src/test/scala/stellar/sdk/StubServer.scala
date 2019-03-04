package stellar.sdk

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{HttpResponse, ResponseEntity}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class StubServer(port: Int = 8002)(implicit ec: ExecutionContext) extends Directives {

  implicit val system = ActorSystem("stub-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  //                              path        params               response
  @volatile var state = Map.empty[String, Map[Map[String, String], ResponseEntity]]

  def expectGet(pathString: String, params: Map[String, String], response: String): Unit = state = {
    state.updated(pathString, state.getOrElse(pathString, Map.empty).updated(params, response))
  }

  val route: Route = ctx => {
    val routes = state.map { case (pathString, responses) =>
      get {
        path(pathString) {
          parameterMap { params =>
            responses.get(params)
              .map(_.withContentType(`application/json`))
              .map(entity => HttpResponse(entity = entity))
              .map(complete(_))
              .getOrElse(
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

  def stop(): Unit = Await.result(binding.map(_.terminate(1.minute)), 1.minute)
}

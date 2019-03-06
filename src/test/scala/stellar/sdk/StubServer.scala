package stellar.sdk

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.ContentTypes.{`text/plain(UTF-8)`, `application/json`}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directives, PathMatcher, PathMatchers, Route}
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import stellar.sdk.StubServer._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class StubServer(port: Int = 8002)(implicit ec: ExecutionContext) extends Directives with LazyLogging {

  implicit val system = ActorSystem("stub-server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // for debugging
  val logUnmatchedRoutes = false

  //                              path        params               response
  @volatile var state = Map.empty[String, Map[Map[String, String], Reply]]

  private def updateState(pathString: String, params: Map[String, String], response: Reply): Unit = this.synchronized {
    state = {
      state.updated(pathString, state.getOrElse(pathString, Map.empty).updated(params, response))
    }
  }

  def expectGet(pathString: String, params: Map[String, String], response: Reply): Unit = {
    updateState(pathString, params, response)
  }

  def forgetGet(pathString: String, params: Map[String, String]): Unit = {
    updateState(pathString, params, ReplyWithNothing)
  }

  def expectGetRedirect(base: String, pathString: String, viaPath: String, params: Map[String, String], response: Reply): Unit = {
    expectGet(pathString, params, response)
    updateState(viaPath, params, ReplyWithRedirect(s"$base/$pathString"))
  }

  def errorOnGet(pathString: String, params: Map[String, String]): Unit = {
    updateState(pathString, params, FreakOut)
  }

  val route: Route = ctx => {
    val routes = state.map { case (pathString, responses) =>
      get {
        path(PathMatchers.separateOnSlashes(pathString)) {
          parameterMap { params =>
            responses.get(params).map {
              case ReplyWithNothing => complete(HttpResponse(404, entity = "nothing here"))
              case ReplyWithJson(response) =>
                complete(HttpResponse(entity = response).mapEntity(_.withContentType(`application/json`)))
              case ReplyWithText(response) =>
                complete(HttpResponse(entity = response).mapEntity(_.withContentType(`text/plain(UTF-8)`)))
              case ReplyWithRedirect(to) => redirect(to, StatusCodes.PermanentRedirect)
              case FreakOut => complete(HttpResponse(StatusCodes.InternalServerError, entity = "I'm freaking out man"))
            }.getOrElse(
              complete(HttpResponse(404, entity = "No such expectation"))
            )
          }
        }
      }
    }

    val withDefaultRoute = routes.toSeq :+ {
      complete {
        if (logUnmatchedRoutes) {
          logger.warn(s"No expectation for $ctx")
          logger.info("Expectations are:")
          state.foreach { case (pathString, responses) =>
            responses.foreach { r =>
              logger.info(s"  $pathString $r")
            }
          }
        }
        HttpResponse(404, entity = "No such expectation")
      }
    }

    concat(withDefaultRoute: _*)(ctx)
  }

  private val binding: Future[ServerBinding] = {
    Http().bindAndHandle(route, "0.0.0.0", port)
  }

  def stop(): Unit = binding.map(_.terminate(1.minute))

}

object StubServer {

  sealed trait Reply

  case object ReplyWithNothing extends Reply

  case class ReplyWithText(s: String) extends Reply

  case class ReplyWithJson(s: String) extends Reply

  case class ReplyWithRedirect(to: String) extends Reply

  case object FreakOut extends Reply

}

package stellar

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import stellar.sdk.model.response.AccountResponse

import scala.language.implicitConversions

package object sdk {

  implicit def accnFromAccnResp(resp: AccountResponse) = resp.toAccount

  object DefaultActorSystem {
    implicit val system: ActorSystem = {
      val conf = ConfigFactory.load().getConfig("scala-stellar-sdk")
      ActorSystem("stellar-sdk", conf)
    }
  }

}
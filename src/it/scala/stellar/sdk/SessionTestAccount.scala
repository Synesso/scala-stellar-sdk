package stellar.sdk

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object SessionTestAccount {

  val accn = {
    val kp = KeyPair.random
    Await.result(TestNetwork.fund(kp), 30 seconds)
    kp
  }

}

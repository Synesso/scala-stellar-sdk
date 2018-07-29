package stellar.sdk

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SessionTestAccount {

  val accnA = newAccount
  val accnB = newAccount

  def newAccount: KeyPair = {
    // #friendbot_example
    val kp = KeyPair.random
    val response = TestNetwork.fund(kp)
    // #friendbot_example
    Await.result(response, 30 seconds)
    kp
  }

  // todo - replace with accn when we can submit manage data operations
  val accWithData = KeyPair.fromSecretSeed("SAOWFZ4OYP5VSAZ7ENZJ2DYP75CRWKYCQ67CMYJVHU5VXPNNBBFJVCOO")

}

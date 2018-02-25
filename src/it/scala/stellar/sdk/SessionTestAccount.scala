package stellar.sdk

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object SessionTestAccount {

  // friendbot is broken again
//  val accn = {
//    val kp = KeyPair.random
//    Await.result(TestNetwork.fund(kp), 30 seconds)
//    kp
//  }

  // GCSIJ7V6PCTJB6YOHANOFUC76GXWUNTYAGCLA5XNUSTZPTELVC34NQ3N
  val accn = KeyPair.fromSecretSeed("SAXMM3DJ7L5MLY753UYSXQEAYWSOBZNRETPPEF2TB7XU4MY5SJPHB5WR")

  // todo - replace with accn when we can submit manage data operations
  val accWithData = KeyPair.fromSecretSeed("SAOWFZ4OYP5VSAZ7ENZJ2DYP75CRWKYCQ67CMYJVHU5VXPNNBBFJVCOO")

}

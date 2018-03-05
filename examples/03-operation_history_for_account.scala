// Fetches a stream of the first (up-to) 100 operations for the given account and prints each one

import scala.concurrent.ExecutionContext.Implicits.global

import stellar.sdk._

val account = KeyPair.fromAccountId("GD3IYBNQ45LXHFABSX4HLGDL7BQA62SVB5NB5O6XMBCITFZOLWLVS22B")

PublicNetwork.operationsByAccount(account).foreach{ stream =>
  stream.take(100).foreach(println)
}

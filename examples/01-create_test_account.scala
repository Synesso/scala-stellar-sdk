// uses FriendBot to create and fund a new account

import stellar.sdk._
import stellar.sdk.resp.FundTestAccountResp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

// generate a new key pair
val kp = KeyPair.random

// ask friendbot to fund it
val result = TestNetwork.fund(kp)

result onComplete {
  case Success(FundTestAccountResp(hash, ledger)) =>
    println(s"Account ${kp.accountId} was funded in ledger $ledger")
  case Failure(t) =>
    println(s"Failed to fund ${kp.accountId}")
    t.printStackTrace()
}

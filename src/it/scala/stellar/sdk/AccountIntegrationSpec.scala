package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.SessionTestAccount.accn
import stellar.sdk.inet.ResourceMissingException
import stellar.sdk.resp.AccountResp

import scala.concurrent.duration._

class AccountIntegrationSpec(implicit ee: ExecutionEnv) extends Specification {

  "account endpoint" >> {
    "fetch account details" >> {
      TestNetwork.account(accn) must beLike[AccountResp] {
        case AccountResp(id, _, _, _, _, _, List(lumens), _) =>
          id mustEqual accn.accountId
          lumens mustEqual Amount.lumens(10000).get
      }.awaitFor(30.seconds)
    }

    "fetch nothing if no account exists" >> {
      TestNetwork.account(KeyPair.random) must throwA[ResourceMissingException].awaitFor(5.seconds)
    }

  }

}

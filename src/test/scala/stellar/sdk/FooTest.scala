package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.domain.DomainInfo

import scala.concurrent.Await
import scala.concurrent.duration._

class FooTest(implicit ee: ExecutionEnv) extends Specification {

  "do it" >> {
    println(Await.result(

      for {
        Some(info) <- DomainInfo.forDomain("keybase.io")
        Some(federation) = info.federationServer
        Some(resolved) <- federation.byName("jem*keybase.io")
      } yield resolved.account

      , 10.seconds))

    ok
  }

}

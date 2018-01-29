package stellar.scala.sdk.inet

import org.specs2.mutable.Specification

class ServerSpec extends Specification {

  "creating a server from string" should {
    "fail when uri is invalid" >> {
      Server("fruit:\\foo") must beFailedTry[Server]
    }
    "succeed when uri is compliant" >> {
      Server("https://horizon.stellar.org") must beSuccessfulTry[Server]
    }
  }

}

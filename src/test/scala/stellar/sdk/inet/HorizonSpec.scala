package stellar.sdk.inet

import org.specs2.mutable.Specification

class HorizonSpec extends Specification {

  "creating a server from string" should {
    "fail when uri is invalid" >> {
      HorizonAccess("fruit:\\foo") must beFailedTry[HorizonAccess]
    }
    "succeed when uri is compliant" >> {
      HorizonAccess("https://horizon.stellar.org") must beSuccessfulTry[HorizonAccess]
    }
  }
}

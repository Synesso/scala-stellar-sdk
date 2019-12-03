package stellar.sdk.model

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

class HorizonCursorSpec extends Specification with ScalaCheck {
  "creating a cursor with a specified number" should {
    "have the correct paramString" >> prop { l: Long =>
      Record(l).paramValue mustEqual l.toString
    }
  }
}

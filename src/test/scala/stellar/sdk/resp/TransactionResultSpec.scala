package stellar.sdk.resp

import org.specs2.mutable.Specification
import stellar.sdk.NativeAmount

class TransactionResultSpec extends Specification {

  "An XDR transaction success" should {
    "be decodable" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGQAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAA=") must
        beSuccessfulTry[TransactionResult](TransactionSuccess(NativeAmount(100)))
    }
  }
}

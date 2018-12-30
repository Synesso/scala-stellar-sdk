package stellar.sdk.result

import org.specs2.mutable.Specification
import stellar.sdk.result.TransactionResult.Code
import stellar.sdk.{ArbitraryInput, ByteArrays, DomainMatchers}

class TransactionResultSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a transaction result" should {
    "serde via xdr bytes" >> prop { r: TransactionResult =>
      r must serdeUsing(TransactionResult.decode)
    }
  }

  "a transaction result code" should {
    "not be constructed with an invalid id" >> {
      Code(-12) must throwA[RuntimeException]
    }
  }

}

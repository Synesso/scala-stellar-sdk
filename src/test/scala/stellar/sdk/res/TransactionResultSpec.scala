package stellar.sdk.res

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, ByteArrays, DomainMatchers}

class TransactionResultSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a transaction result" should {
    "serde via xdr bytes" >> prop { r: TransactionResult =>
      r must serdeUsing(TransactionResult.decode)
    }
  }

}

package stellar.sdk.model.result

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{OperationResultCode, OperationResult => XOperationResult}

class OperationResultSpec extends Specification with ArbitraryInput with DomainMatchers {
  "operation results" should {
    "serde via xdr bytes" >> prop { or: OperationResult =>
      OperationResult.decodeXdr(or.xdr) mustEqual or
    }.set(minTestsOk = 2500)
  }
}
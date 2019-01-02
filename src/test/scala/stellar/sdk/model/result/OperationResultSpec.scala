package stellar.sdk.model.result

import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

class OperationResultSpec extends Specification with ArbitraryInput with DomainMatchers {

  "operation results" should {
    "serde via xdr bytes" >> prop { or: OperationResult =>
      val (remaining, decoded) = OperationResult.decode.run(or.encode).value
      decoded mustEqual or
      remaining must beEmpty
    }
  }

  "account merge results" should {
    "serde via xdr bytes" >> prop { r: AccountMergeResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "allow trust results" should {
    "serde via xdr bytes" >> prop { r: AllowTrustResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "bump sequence results" should {
    "serde via xdr bytes" >> prop { r: BumpSequenceResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "change trust results" should {
    "serde via xdr bytes" >> prop { r: ChangeTrustResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "change trust results" should {
    "serde via xdr bytes" >> prop { r: ChangeTrustResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "create account results" should {
    "serde via xdr bytes" >> prop { r: CreateAccountResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "create passive offer results" should {
    "serde via xdr bytes" >> prop { r: CreatePassiveOfferResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "inflation results" should {
    "serde via xdr bytes" >> prop { r: InflationResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "manage data results" should {
    "serde via xdr bytes" >> prop { r: ManageDataResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "manage offer results" should {
    "serde via xdr bytes" >> prop { r: ManageOfferResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "path payment results" should {
    "serde via xdr bytes" >> prop { r: PathPaymentResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "payment results" should {
    "serde via xdr bytes" >> prop { r: PaymentResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

  "set options results" should {
    "serde via xdr bytes" >> prop { r: SetOptionsResult =>
      r should serdeUsing(OperationResult.decode)
    }
  }

}

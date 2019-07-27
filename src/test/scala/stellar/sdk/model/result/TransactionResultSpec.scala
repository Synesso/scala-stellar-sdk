package stellar.sdk.model.result

import org.scalacheck.Arbitrary
import org.specs2.mutable.Specification
import stellar.sdk.model.ledger.OfferEntry
import stellar.sdk.model.{Amount, Asset, NativeAmount, Price}
import stellar.sdk.model.result.TransactionResult._
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair}

import scala.util.Try

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

  "An XDR transaction success" should {
    "be decodable" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGQAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAB////+wAAAAA=") mustEqual
        TransactionSuccess(NativeAmount(100), Seq(CreateAccountSuccess, PaymentNoDestination))
    }
  }

  "a transaction success" should {
    "always mark sequenceUpdates as true" >> prop { t: TransactionSuccess =>
      t.sequenceUpdated must beTrue
    }
  }

  "An XDR transaction failure" should {
    "be decodable for failure due to bad operation" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT/////AAAAAQAAAAAAAAAB/////AAAAAA=") mustEqual
        TransactionFailure(NativeAmount(100), Seq(PaymentSourceNotAuthorised))
    }
    "be decodable for txn too early" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////+AAAAAA==") mustEqual
        TransactionNotAttempted(SubmittedTooEarly, NativeAmount(100))
    }
    "be decodable for txn too late" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////9AAAAAA==") mustEqual
        TransactionNotAttempted(SubmittedTooLate, NativeAmount(100))
    }
    "be decodable for bad auth" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////6AAAAAA==") mustEqual
        TransactionNotAttempted(BadAuthorisation, NativeAmount(100))
    }
    "be decodable for missing operations" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////8AAAAAA==") mustEqual
        TransactionNotAttempted(NoOperations, NativeAmount(100))
    }
    "be decodable for bad sequence numbers" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////7AAAAAA==") mustEqual
        TransactionNotAttempted(BadSequenceNumber, NativeAmount(100))
    }
    "be decodable for insufficient balance" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////5AAAAAA==") mustEqual
        TransactionNotAttempted(InsufficientBalance, NativeAmount(100))
    }
    "be decodable for missing source account" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////4AAAAAA==") mustEqual
        TransactionNotAttempted(SourceAccountNotFound, NativeAmount(100))
    }
    "be decodable for insufficient fee" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////3AAAAAA==") mustEqual
        TransactionNotAttempted(InsufficientFee, NativeAmount(100))
    }
    "be decodable for extraneous signatures" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////2AAAAAA==") mustEqual
        TransactionNotAttempted(UnusedSignatures, NativeAmount(100))
    }
    "be decodable for other reasons" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGT////1AAAAAA==") mustEqual
        TransactionNotAttempted(UnspecifiedInternalError, NativeAmount(100))
    }
    "return failure when not decodable" >> {
      Try(TransactionResult.decodeXDR("foo")) must beFailedTry[TransactionResult]
    }
  }

  "a transaction not successful" should {
    "mark sequenceUpdates as true only when fee is not zero" >> prop { t: TransactionNotSuccessful =>
      t.sequenceUpdated mustEqual t.feeCharged.units != 0
    }
  }
}

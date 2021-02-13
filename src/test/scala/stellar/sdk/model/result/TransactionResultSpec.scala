package stellar.sdk.model.result

import okio.ByteString
import org.specs2.mutable.Specification
import org.stellar.xdr.TransactionResultCode
import stellar.sdk.model.NativeAmount
import stellar.sdk.{ArbitraryInput, DomainMatchers}

import scala.util.Try

class TransactionResultSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a transaction result" should {
    "serde via xdr bytes" >> prop { r: TransactionResult =>
      TransactionResult.decodeXdr(r.xdr) mustEqual r
    }
  }

  "An XDR transaction success" should {
    "be decodable" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGQAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAB////+wAAAAA=") mustEqual
        TransactionSuccess(NativeAmount(100), Seq(CreateAccountSuccess, PaymentNoDestination), ByteString.EMPTY)
    }
  }

  "a transaction success" should {
    "always mark sequenceUpdates as true" >> prop { t: TransactionSuccess =>
      t.sequenceUpdated must beTrue
    }
  }

  "An XDR transaction failure" should {
    "be decodable for failure due to bad operation" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT/////AAAAAQAAAAAAAAAB/////AAAAAA=") mustEqual
        TransactionFailure(NativeAmount(100), Seq(PaymentSourceNotAuthorised))
    }
    "be decodable for txn too early" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////+AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txTOO_EARLY, NativeAmount(100), None)
    }
    "be decodable for txn too late" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////9AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txTOO_LATE, NativeAmount(100), None)
    }
    "be decodable for bad auth" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////6AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txBAD_AUTH, NativeAmount(100), None)
    }
    "be decodable for missing operations" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////8AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txMISSING_OPERATION, NativeAmount(100), None)
    }
    "be decodable for bad sequence numbers" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////7AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txBAD_SEQ, NativeAmount(100), None)
    }
    "be decodable for insufficient balance" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////5AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txINSUFFICIENT_BALANCE, NativeAmount(100), None)
    }
    "be decodable for missing source account" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////4AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txNO_ACCOUNT, NativeAmount(100), None)
    }
    "be decodable for insufficient fee" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////3AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txINSUFFICIENT_FEE, NativeAmount(100), None)
    }
    "be decodable for extraneous signatures" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////2AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txBAD_AUTH_EXTRA, NativeAmount(100), None)
    }
    "be decodable for other reasons" >> {
      TransactionResult.decodeXdrString("AAAAAAAAAGT////1AAAAAA==") mustEqual
        TransactionNotAttempted(TransactionResultCode.txINTERNAL_ERROR, NativeAmount(100), None)
    }
    "return failure when not decodable" >> {
      Try(TransactionResult.decodeXdrString("foo")) must beFailedTry[TransactionResult]
    }
  }

  "a transaction not successful" should {
    "mark sequenceUpdates as true only when fee is not zero" >> prop { t: TransactionNotSuccessful =>
      t.sequenceUpdated mustEqual t.feeCharged.units != 0
    }
  }
}

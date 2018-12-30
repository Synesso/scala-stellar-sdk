package stellar.sdk.response

import org.specs2.mutable.Specification
import stellar.sdk.{ByteArrays, NativeAmount}
import stellar.sdk.result.TransactionResult._
import stellar.sdk.result._

import scala.util.Try

class TransactionResultSpec extends Specification {

  "An XDR transaction success" should {
    "be decodable" >> {
      TransactionResult.decodeXDR("AAAAAAAAAGQAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAB////+wAAAAA=") mustEqual
        TransactionSuccess(NativeAmount(100), Seq(CreateAccountSuccess, PaymentNoDestination))
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

  // todo - implement
/*
  "An XDR transaction meta" should {
    "be decodable" >> {
      Try(TxResult.decodeMetaXDR(
        "AAAAAAAAAAEAAAADAAAAAAB18EMAAAAAAAAAALkbMPgKFokh2tU1rsRPDtL3uWLY9iSRNbA6yzSbKbMbAAAAAACYloAAdfBDAA" +
          "AAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAwB18EMAAAAAAAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8" +
          "MmbfYAAAAF0h255wAdeiJAAAAAQAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAQB18EMAAAAAAAAAAJYHU4BtUa8ACOZZ" +
          "zHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAF0feURwAdeiJAAAAAQAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAA")) must
        beSuccessfulTry[TransactionMeta]
    }
    "return failure when not decodable" >> {
      Try(TxResult.decodeMetaXDR("foo")) must beFailedTry[TransactionMeta]
    }
  }
*/

}

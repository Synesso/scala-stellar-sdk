package stellar.sdk.resp

import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.TransactionResult

class TransactionResultSpec extends Specification {

  "An XDR transaction success" should {
    "be decodable" >> {
      TxResult.decodeXDR("AAAAAAAAAGQAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAB////+wAAAAA=") must
        beSuccessfulTry[TransactionResult]
    }
  }

  "An XDR transaction failure" should {
    "be decodable for failure due to bad operation" >> {
      TxResult.decodeXDR("AAAAAAAAAGT/////AAAAAQAAAAAAAAAB/////AAAAAA=") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for txn too early" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////+AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for txn too late" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////9AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for bad auth" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////6AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for missing operations" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////8AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for bad sequence numbers" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////7AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for insufficient balance" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////5AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for insufficient fee" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////4AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for missing source account" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////3AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for extraneous signatures" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////2AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
    "be decodable for other reasons" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////1AAAAAA==") must
        beSuccessfulTry[TransactionResult]
    }
  }
}

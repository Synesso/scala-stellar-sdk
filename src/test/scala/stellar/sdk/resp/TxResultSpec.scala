package stellar.sdk.resp

import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.{PaymentResultCode, TransactionMeta, TransactionResult, TransactionResultCode}

import scala.util.Try

class TxResultSpec extends Specification {

  "An XDR transaction success" should {
    "be decodable" >> {
      TxResult.decodeXDR("AAAAAAAAAGQAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAB////+wAAAAA=")
        .getResult.getResults must haveSize(2)
    }
  }

  "An XDR transaction failure" should {
    "be decodable for failure due to bad operation" >> {
      TxResult.decodeXDR("AAAAAAAAAGT/////AAAAAQAAAAAAAAAB/////AAAAAA=")
        .getResult.getResults.head.getTr.getPaymentResult.getDiscriminant mustEqual PaymentResultCode.PAYMENT_SRC_NOT_AUTHORIZED
    }
    "be decodable for txn too early" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////+AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txTOO_EARLY
    }
    "be decodable for txn too late" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////9AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txTOO_LATE
    }
    "be decodable for bad auth" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////6AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txBAD_AUTH
    }
    "be decodable for missing operations" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////8AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txMISSING_OPERATION
    }
    "be decodable for bad sequence numbers" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////7AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txBAD_SEQ
    }
    "be decodable for insufficient balance" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////5AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txINSUFFICIENT_BALANCE
    }
    "be decodable for missing source account" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////4AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txNO_ACCOUNT
    }
    "be decodable for insufficient fee" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////3AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txINSUFFICIENT_FEE
    }
    "be decodable for extraneous signatures" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////2AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txBAD_AUTH_EXTRA
    }
    "be decodable for other reasons" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////1AAAAAA==").getResult.getDiscriminant mustEqual TransactionResultCode.txINTERNAL_ERROR
    }
    "return failure when not decodable" >> {
      Try(TxResult.decodeXDR("foo")) must beFailedTry[TransactionResult]
    }
  }

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

}

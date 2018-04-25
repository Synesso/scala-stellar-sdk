package stellar.sdk.resp

import org.specs2.mutable.Specification
import stellar.sdk.NativeAmount

class TransactionResultSpec extends Specification {

  "An XDR transaction success" should {
    "be decodable" >> {
      TxResult.decodeXDR("AAAAAAAAAGQAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAA=") must
        beSuccessfulTry[TxResult](TxSuccess(NativeAmount(100)))
      // todo - add operations

    }
  }

  "An XDR transaction failure" should {
    "be decodable for failure due to bad operation" >> {
      TxResult.decodeXDR("AAAAAAAAAGT/////AAAAAQAAAAAAAAAB/////AAAAAA=") must
        beSuccessfulTry[TxResult](TxFailBadOperation(NativeAmount(100)))
      // todo - add operations
    }
    "be decodable for txn too early" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////+AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailTooEarly(NativeAmount(100)))
    }
    "be decodable for txn too late" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////9AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailTooLate(NativeAmount(100)))
    }
    "be decodable for bad auth" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////6AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailBadSignatures(NativeAmount(100)))
    }
    "be decodable for missing operations" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////8AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailMissingOperation(NativeAmount(100)))
    }
    "be decodable for bad sequence numbers" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////7AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailBadSequence(NativeAmount(100)))
    }
    "be decodable for insufficient balance" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////5AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailInsufficientBalance(NativeAmount(100)))
    }
    "be decodable for insufficient fee" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////4AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailNoSourceAccount(NativeAmount(100)))
    }
    "be decodable for missing source account" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////3AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailInsufficientFee(NativeAmount(100)))
    }
    "be decodable for extraneous signatures" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////2AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailUnusedSignatures(NativeAmount(100)))
    }
    "be decodable for other reasons" >> {
      TxResult.decodeXDR("AAAAAAAAAGT////1AAAAAA==") must
        beSuccessfulTry[TxResult](TxFailOther(-11, NativeAmount(100)))
    }
  }

  /*

  // for creation of XDR string test input

  def foo(xdr: XDRTransactionResult) = {
    val baos = new ByteArrayOutputStream
    val os = new XdrDataOutputStream(baos)
    XDRTransactionResult.encode(os, xdr)
    base64(baos.toByteArray)
  }

  "x" should {
    "y" >> {
      val tr = new XDRTransactionResult()
      tr.setFeeCharged(int64(100))
      tr.setExt(new TransactionResultExt)
      tr.getExt.setDiscriminant(0)
      tr.setResult(new TransactionResultResult)
      tr.getResult.setDiscriminant(TransactionResultCode.txINSUFFICIENT_FEE)
      println(foo(tr))
      ko
    }
  }
*/
}

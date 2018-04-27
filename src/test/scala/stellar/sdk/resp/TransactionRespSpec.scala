package stellar.sdk.resp

import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.{OperationType, TransactionResult, TransactionResultCode}
import stellar.sdk.ByteArrays.bytesToHex
import stellar.sdk._
import stellar.sdk.op.CreateAccountOperation

class TransactionRespSpec extends Specification {

  implicit val network = TestNetwork

  "a transaction response" should {
    "provide access to the signed transaction via XDR decoding" >> {
      TransactionResp("", 1, "AAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAZAB16IkAAAABAAAAAAAAAAAAAAABAAAAA" +
        "AAAAAAAAAAAuRsw+AoWiSHa1TWuxE8O0ve5Ytj2JJE1sDrLNJspsxsAAAAAAJiWgAAAAAAAAAABDJm32AAAAEDnDn8POBeTu0v5Hj6VCVB" +
        "KABHtap9ut+HH0+taBQsDPNLA+WXfiwrq1hG5cEQP0qTHG59vkmyjxcejqjz7dPwO", "", "").transaction must
        beSuccessfulTry[SignedTransaction].like {
          case SignedTransaction(txn, signatures, hash) =>
            txn.source.publicKey.accountId mustEqual "GCLAOU4ANVI26AAI4ZM4Y4QI4PQW2EQELL3FAZE6MI7I2DYMTG35R35E"
            txn.source.sequenceNumber mustEqual 33188247383310337L
            txn.operations mustEqual Seq(
              CreateAccountOperation(
                KeyPair.fromAccountId("GC4RWMHYBILISIO22U225RCPB3JPPOLC3D3CJEJVWA5MWNE3FGZRXTFA"),
                NativeAmount(10000000)
              )
            )
            txn.timeBounds must beNone
            txn.memo mustEqual NoMemo
            txn.fee must beSome(txn.calculatedFee)
            signatures.map(_.getSignature.getSignature).map(bytesToHex) mustEqual Seq("E70E7F0F381793BB4BF91E3E950950" +
              "4A0011ED6A9F6EB7E1C7D3EB5A050B033CD2C0F965DF8B0AEAD611B970440FD2A4C71B9F6F926CA3C5C7A3AA3CFB74FC0E"
            )
            signatures.map(_.getHint.getSignatureHint).map(bytesToHex) mustEqual Seq("0C99B7D8")
            bytesToHex(hash) mustEqual "BA68C0112AFE25A2FEA9A6E7926A4AEF9FF12FB627EC840840541813AAA695DB"
        }
    }

    "provide access to the XDR Transaction Result" >> {
      TransactionResp("", 1, "", "AAAAAAAAAGQAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAB////+wAAAAA=", "").result must
        beSuccessfulTry[TransactionResult].like { case tr: TransactionResult =>
          tr.getFeeCharged.getInt64 mustEqual 100
          tr.getExt.getDiscriminant mustEqual 0
          tr.getResult.getDiscriminant mustEqual TransactionResultCode.txSUCCESS
          tr.getResult.getResults must haveSize(2)
          tr.getResult.getResults.head.getTr.getDiscriminant mustEqual OperationType.CREATE_ACCOUNT
          tr.getResult.getResults.last.getTr.getDiscriminant mustEqual OperationType.PAYMENT
        }
    }
  }

}

/*
{
"_links": {
  "transaction": {
    "href": "https://horizon-testnet.stellar.org/transactions/ba68c0112afe25a2fea9a6e7926a4aef9ff12fb627ec840840541813aaa695db"
  }
},
"hash": "ba68c0112afe25a2fea9a6e7926a4aef9ff12fb627ec840840541813aaa695db",
"ledger": 7729219,
"envelope_xdr": "AAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAZAB16IkAAAABAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAuRsw+AoWiSHa1TWuxE8O0ve5Ytj2JJE1sDrLNJspsxsAAAAAAJiWgAAAAAAAAAABDJm32AAAAEDnDn8POBeTu0v5Hj6VCVBKABHtap9ut+HH0+taBQsDPNLA+WXfiwrq1hG5cEQP0qTHG59vkmyjxcejqjz7dPwO",
"result_xdr": "AAAAAAAAAGQAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAA=",
"result_meta_xdr": "AAAAAAAAAAEAAAADAAAAAAB18EMAAAAAAAAAALkbMPgKFokh2tU1rsRPDtL3uWLY9iSRNbA6yzSbKbMbAAAAAACYloAAdfBDAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAwB18EMAAAAAAAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAF0h255wAdeiJAAAAAQAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAQB18EMAAAAAAAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAF0feURwAdeiJAAAAAQAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAA"
}


    val is = new XdrDataInputStream(new ByteArrayInputStream(baos.toByteArray))
    XDRMemo.decode(is) must beEquivalentTo(xdrMemo)


 */

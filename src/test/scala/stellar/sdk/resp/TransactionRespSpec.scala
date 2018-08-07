package stellar.sdk.resp

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.{OperationType, TransactionMeta, TransactionResult, TransactionResultCode}
import stellar.sdk.ByteArrays.{base64, bytesToHex}
import stellar.sdk._
import stellar.sdk.op.CreateAccountOperation
import org.json4s.JsonDSL._

class TransactionRespSpec extends Specification with ArbitraryInput with DomainMatchers {

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))

  "a transaction post response" should {
    "provide access to the signed transaction via XDR decoding" >> {
      TransactionPostResp("", 1, "AAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAZAB16IkAAAABAAAAAAAAAAAAAAABAAAAA" +
        "AAAAAAAAAAAuRsw+AoWiSHa1TWuxE8O0ve5Ytj2JJE1sDrLNJspsxsAAAAAAJiWgAAAAAAAAAABDJm32AAAAEDnDn8POBeTu0v5Hj6VCVB" +
        "KABHtap9ut+HH0+taBQsDPNLA+WXfiwrq1hG5cEQP0qTHG59vkmyjxcejqjz7dPwO", "", "").transaction must
        beLike {
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
      TransactionPostResp("", 1, "", "AAAAAAAAAGQAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAB////+wAAAAA=", "").result must
        beLike { case tr: TransactionResult =>
          tr.getFeeCharged.getInt64 mustEqual 100
          tr.getExt.getDiscriminant mustEqual 0
          tr.getResult.getDiscriminant mustEqual TransactionResultCode.txSUCCESS
          tr.getResult.getResults must haveSize(2)
          tr.getResult.getResults.head.getTr.getDiscriminant mustEqual OperationType.CREATE_ACCOUNT
          tr.getResult.getResults.last.getTr.getDiscriminant mustEqual OperationType.PAYMENT
        }
    }

    "provide access to the XDR Transaction Result Meta" >> {
      TransactionPostResp("", 1, "", "", "AAAAAAAAAAEAAAACAAAAAAAACVIAAAAAAAAAAPV0vlN3VR04WFNx2dsyXUyxlcIhv99+eHwdMjqmf" +
        "MHaAAAAF0h26AAAAAlSAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAQAACVIAAAAAAAAAAGXNhLrhGtltTwCpmqlarh7" +
        "s1DB2hIkbP//jgzn4Fos/AAHGqAnsV5wAAAk9AAAAAQAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAA").resultMeta must
        beLike { case tm: TransactionMeta =>
          tm.getDiscriminant mustEqual 0
          tm.getOperations must haveSize(1)
          tm.getOperations.head.getChanges.getLedgerEntryChanges must haveSize(2)
        }
    }

    "deserialise from JSON" >> prop { tpr: TransactionPostResp =>
      val json = ("hash" -> tpr.hash) ~ ("ledger" -> tpr.ledger) ~ ("envelope_xdr" -> tpr.envelopeXDR) ~
        ("result_xdr" -> tpr.resultXDR) ~ ("result_meta_xdr" -> tpr.resultMetaXDR)

      implicit val fmt = org.json4s.DefaultFormats + TransactionPostRespDeserializer
      json.extract[TransactionPostResp] mustEqual tpr
    }
  }

  "a transaction history response" should {
    "deserialise from JSON" >> prop { thr: TransactionHistoryResp =>
      val (memoType, memo) = thr.memo match {
        case NoMemo => "none" -> None
        case MemoId(id) => "id" -> Some(s"$id")
        case MemoText(t) => "text" -> Some(t)
        case m: MemoWithHash => "hash" -> Some(base64(m.bs))
      }
      val json = memo.foldLeft(("hash" -> thr.hash) ~ ("ledger" -> thr.ledger) ~
        ("created_at" -> formatter.format(thr.createdAt)) ~
        ("source_account" -> thr.account.accountId) ~ ("source_account_sequence" -> thr.sequence) ~
        ("fee_paid" -> thr.feePaid) ~ ("operation_count" -> thr.operationCount) ~ ("signatures" -> thr.signatures) ~
        ("memo_type" -> memoType) ~ ("envelope_xdr" -> thr.envelopeXDR) ~ ("result_xdr" -> thr.resultXDR) ~
        ("result_meta_xdr" -> thr.resultMetaXDR) ~ ("fee_meta_xdr" -> thr.feeMetaXDR)) {
        case (js, memoText) => js ~ ("memo" -> memoText)
      }

      implicit val fmt = org.json4s.DefaultFormats + TransactionHistoryRespDeserializer
      json.extract[TransactionHistoryResp] must beEquivalentTo(thr)
    }
  }

}

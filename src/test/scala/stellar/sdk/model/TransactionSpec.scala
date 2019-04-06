package stellar.sdk.model

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk.model.op.{CreateAccountOperation, Operation, PaymentOperation}
import stellar.sdk.util.ByteArrays.bytesToHex
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair, model}

class TransactionSpec extends Specification with ArbitraryInput with DomainMatchers {

  "the default transaction fee" should {
    "be equal to 100 * the quantity of operations" >> prop { (source: Account, ops: Seq[Operation]) =>
      model.Transaction(source, ops, NoMemo).calculatedFee mustEqual NativeAmount(ops.size * 100)
    }.setGen2(Gen.nonEmptyListOf(genOperation))
  }

  "a transaction" should {

    "serde via xdr bytes" >> prop { transaction: Transaction =>
      val (remaining, actual) = Transaction.decode.run(transaction.encode).value
      actual must beEquivalentTo(transaction)
      remaining must beEmpty
    }

    "allow adding of operations one at a time" >> prop { (source: Account, ops: Seq[Operation]) =>
      val expected = model.Transaction(source, ops, NoMemo)
      val actual = ops.foldLeft(model.Transaction(source, memo = NoMemo)) { case (txn, op) => txn add op }
      actual mustEqual expected
    }

    "allow signing of the transaction one signature at a time" >> prop { (transaction: Transaction, signers: Seq[KeyPair]) =>
      val expected = transaction.sign(signers.head, signers.tail: _*)
      val actual: SignedTransaction = signers match {
        case Seq(only) => transaction.sign(only)
        case h +: t => t.foldLeft(transaction.sign(h)) { case (txn, kp) => txn.sign(kp) }
      }
      actual must beEquivalentTo(expected)
    }.setGen2(Gen.nonEmptyListOf(genKeyPair))

    "express signed transaction envelope as base64" >> {
      // specific example lifted from java sdk
      val source = KeyPair.fromSecretSeed("SCH27VUZZ6UAKB67BDNF6FA42YMBMQCBKXWGMFD5TZ6S5ZZCZFLRXKHS")
      val dest = KeyPair.fromAccountId("GDW6AUTBXTOC7FIKUO5BOO3OGLK4SF7ZPOBLMQHMZDI45J2Z6VXRB5NR")
      val seqNum = 2908908335136769L

      val account = Account(source, seqNum)
      val txn = Transaction(
        source = account,
        operations = Seq(CreateAccountOperation(dest, NativeAmount(20000000000L)))
      ).sign(source)

      txn.encodeXDR mustEqual "AAAAAF7FIiDToW1fOYUFBC0dmyufJbFTOa2GQESGz+S2h5ViAAAAZAAKVaMAAAABAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAA7eBSYbzcL5UKo7oXO24y1ckX+XuCtkDsyNHOp1n1bxAAAAAEqBfIAAAAAAAAAAABtoeVYgAAAEDLki9Oi700N60Lo8gUmEFHbKvYG4QSqXiLIt9T0ru2O5BphVl/jR9tYtHAD+UeDYhgXNgwUxqTEu1WukvEyYcD"
      txn.transaction.source must beEquivalentTo(account)
      txn.transaction.calculatedFee mustEqual NativeAmount(100)
    }
  }

  "signing a transaction" should {
    "add a signature to that transaction" >> prop { (source: Account, op: Operation, signers: Seq[KeyPair]) =>
      val signatures = model.Transaction(source, Seq(op), NoMemo).sign(signers.head, signers.tail: _*).signatures
      signatures must haveSize(signers.length)
    }.setGen3(Gen.nonEmptyListOf(genKeyPair))
  }

/*
  "a signed transaction" should {
    "serialise to xdr" >> prop { (t: Transaction, signer: KeyPair) =>
      t.sign(signer).toXDR must haveClass[TransactionEnvelope]
    }
  }
*/

/*
  "an unsigned transaction" should {
    "serialise to xdr" >> prop { txn: Transaction =>
      val xdr = txn.toXDR
      xdr.getExt.getDiscriminant mustEqual 0
      xdr.getFee.getUint32 mustEqual txn.calculatedFee.units
      xdr.getSeqNum.getSequenceNumber.getInt64 mustEqual txn.source.sequenceNumber
      xdr.getSourceAccount.getAccountID must beEquivalentTo(txn.source.publicKey.getXDRPublicKey)
      xdr.getMemo must beEquivalentTo(txn.memo.toXDR)
      Option(xdr.getTimeBounds) must beLike {
        case None => txn.timeBounds must beNone
        case Some(tb1) =>
          txn.timeBounds must beSome[TimeBounds].like {
            case TimeBounds(start, end) =>
              tb1.getMinTime.getUint64 mustEqual start.toEpochMilli
              tb1.getMaxTime.getUint64 mustEqual end.toEpochMilli
          }
      }
      forall(txn.operations.zip(xdr.getOperations.map(Operation.fromXDR))) {
        case (expected: Operation, actualTry: Try[Operation]) =>
          actualTry must beSuccessfulTry[Operation].like { case actual =>
            actual must beEquivalentTo(expected)
          }
      }
    }

    "ser/de to/from xdr" >> prop { txn: Transaction =>
      Transaction.fromXDR(txn.toXDR) must beLike {
        case Transaction(source, ops, memo, timeBounds, fee) =>
          source.publicKey.accountId mustEqual txn.source.publicKey.accountId
          source.sequenceNumber mustEqual txn.source.sequenceNumber
          ops mustEqual txn.operations
          memo must beEquivalentTo(txn.memo)
          timeBounds mustEqual txn.timeBounds
          fee mustEqual (txn.fee orElse Some(txn.calculatedFee))
      }
    }
  }
*/

  "decoding transaction from xdr string" should {
    "be successful for unsigned transactions" >> prop { txn: Transaction =>
      // #xdr_serde_example
      val encoded: String = txn.encodeXDR
      val decoded: Transaction = Transaction.decodeXDR(encoded)
      decoded must beEquivalentTo(txn)
      // #xdr_serde_example
    }

    "be successful for signed transactions" >> prop { signedTxn: SignedTransaction =>
      // #xdr_signed_serde_example
      val encoded: String = signedTxn.encodeXDR
      val decoded: SignedTransaction = SignedTransaction.decodeXDR(encoded)
      decoded must beEquivalentTo(signedTxn)
      // #xdr_signed_serde_example
    }

    "be successful using sample signed data" >> {
      val sample = "AAAAAAEMy3/N735+S8/jcLYweVCmRxnN2QqWCvGGbxlhX5v3AAAAZAB4Dl4AAAADAAAAAAAAAAEAAAAXSGkgWnksIGhlcmVzI" +
        "GFuIGFuZ3BhbyEAAAAAAQAAAAAAAAABAAAAAK5TNRH+gV9qHLIuWk99Epe7OYH6l7cSXKW18R9DFoIDAAAAAAAAAAAAmJaAAAAAAAAAAAFhX" +
        "5v3AAAAQHx9LVy0EsDozAxndsy+D6E2bWmTAMmnhLoFqf2FfoRAMXjC9BW16ZQlOR+wWH5PSKnz22QpAxY4gMkJvH8LCwQ="

      SignedTransaction.decodeXDR(sample) must beLike {
        case SignedTransaction(txn, signatures) =>
          txn mustEqual Transaction(
            Account(
              KeyPair.fromAccountId("GAAQZS37ZXXX47SLZ7RXBNRQPFIKMRYZZXMQVFQK6GDG6GLBL6N7PBYD"),
              33792794094993411L
            ), Seq(
              PaymentOperation(
                KeyPair.fromAccountId("GCXFGNIR72AV62Q4WIXFUT35CKL3WOMB7KL3OES4UW27CH2DC2BAHMZH"),
                NativeAmount(10000000)
              )
            ), MemoText("Hi Zy, heres an angpao!"), None, Some(NativeAmount(100))
          )

          signatures.map(_.hint).map(bytesToHex(_)) mustEqual Seq("615F9BF7")
          signatures.map(_.data).map(bytesToHex(_)) mustEqual Seq("7C7D2D5CB412C0E8CC0C6776CC" +
            "BE0FA1366D699300C9A784BA05A9FD857E84403178C2F415B5E99425391FB0587E4F48A9F3DB642903163880C909BC7F0B0B04")
          bytesToHex(txn.hash) mustEqual "7D91CFC50E907A677F769E6DB82BDB958D148D810955C597AA7A4B24AE97475B"

      }
    }
  }
}

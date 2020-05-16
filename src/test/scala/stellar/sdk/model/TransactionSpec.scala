package stellar.sdk.model

import org.scalacheck.{Arbitrary, Gen}
import org.specs2.mutable.Specification
import stellar.sdk.model.TimeBounds.Unbounded
import stellar.sdk.model.op.{CreateAccountOperation, Operation, PaymentOperation}
import stellar.sdk.util.ByteArrays.bytesToHex
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair, model}

class TransactionSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a transaction" should {

    "serde via xdr bytes" >> prop { transaction: Transaction =>
      transaction must serdeUsing(Transaction.decode)
    }

    "allow adding of operations one at a time" >> prop { (source: Account, ops: Seq[Operation]) =>
      val expected = model.Transaction(source, ops, NoMemo, timeBounds = Unbounded, maxFee = NativeAmount(100))
      val actual = ops.foldLeft(model.Transaction(source, memo = NoMemo, timeBounds = Unbounded, maxFee = NativeAmount(100))) {
        case (txn, op) => txn add op
      }
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

    "calc the minFee as 100 stroops * the quantity of operations" >> prop { (source: Account, ops: Seq[Operation]) =>
      model.Transaction(source, ops, NoMemo, timeBounds = Unbounded, maxFee = NativeAmount(100)).minFee mustEqual NativeAmount(ops.size * 100)
    }.setGen2(Gen.nonEmptyListOf(genOperation))

    "disallow signing if the maxFee is insufficient" >> prop { (source: Account, ops: Seq[Operation], signer: KeyPair) =>
      val maxFee = NativeAmount(ops.size * 100 - 1)
      model.Transaction(source, ops, NoMemo, timeBounds = Unbounded, maxFee = maxFee).sign(signer) must throwAn[AssertionError]
    }.setGen2(Gen.nonEmptyListOf(genOperation))

    "express signed transaction envelope as base64" >> {
      // specific example lifted from java sdk
      val source = KeyPair.fromSecretSeed("SCH27VUZZ6UAKB67BDNF6FA42YMBMQCBKXWGMFD5TZ6S5ZZCZFLRXKHS")
      val dest = KeyPair.fromAccountId("GDW6AUTBXTOC7FIKUO5BOO3OGLK4SF7ZPOBLMQHMZDI45J2Z6VXRB5NR")
      val accountId = AccountId(source.publicKey)
      val seqNum = 2908908335136769L

      val account = Account(accountId, seqNum)
      val txn = Transaction(
        source = account,
        operations = Seq(CreateAccountOperation(dest.toAccountId, NativeAmount(20000000000L))),
        timeBounds = Unbounded,
        maxFee = NativeAmount(100)
      ).sign(source)

      txn.encodeXDR mustEqual "AAAAAF7FIiDToW1fOYUFBC0dmyufJbFTOa2GQESGz+S2h5ViAAAAZAAKVaMAAAABAAAAAAAAAAAAAAABAAAA" +
        "AAAAAAAAAAAA7eBSYbzcL5UKo7oXO24y1ckX+XuCtkDsyNHOp1n1bxAAAAAEqBfIAAAAAAAAAAABtoeVYgAAAEDLki9Oi700N60Lo8gUmE" +
        "FHbKvYG4QSqXiLIt9T0ru2O5BphVl/jR9tYtHAD+UeDYhgXNgwUxqTEu1WukvEyYcD"
      txn.transaction.source must beEquivalentTo(account)
    }
  }

  "signing a transaction with a keypair" should {
    "add a signature to that transaction" >> prop { (source: Account, op: Operation, signers: Seq[KeyPair]) =>
      val signatures = model.Transaction(source, Seq(op), NoMemo, timeBounds = Unbounded, maxFee = NativeAmount(100))
        .sign(signers.head, signers.tail: _*).signatures
      signatures must haveSize(signers.length)
    }.setGen3(Gen.nonEmptyListOf(genKeyPair))
  }

  "signing a transaction with a pre-image" should {
    "add a signature to that transaction" >> prop { (source: Account, op: Operation, preImages: Seq[Array[Byte]]) =>
      val transaction = model.Transaction(source, Seq(op), NoMemo, timeBounds = Unbounded, maxFee = NativeAmount(100))
      val signedTxn = transaction.sign(preImages.head.toSeq)
      preImages.tail.map(_.toSeq).foldLeft(signedTxn) { _ sign _ }.signatures must haveSize(preImages.length)
    }.setGen3(Gen.nonEmptyListOf(Gen.containerOf[Array, Byte](Arbitrary.arbByte.arbitrary)))
  }

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
        case SignedTransaction(txn, signatures, feeBump) =>
          txn mustEqual Transaction(
            Account(
              AccountId(KeyPair.fromAccountId("GAAQZS37ZXXX47SLZ7RXBNRQPFIKMRYZZXMQVFQK6GDG6GLBL6N7PBYD").publicKey),
              33792794094993411L
            ), Seq(
              PaymentOperation(
                KeyPair.fromAccountId("GCXFGNIR72AV62Q4WIXFUT35CKL3WOMB7KL3OES4UW27CH2DC2BAHMZH").toAccountId,
                NativeAmount(10000000)
              )
            ), MemoText("Hi Zy, heres an angpao!"), Unbounded, NativeAmount(100)
          )

          signatures.map(_.hint.toIndexedSeq).map(bytesToHex(_)) mustEqual Seq("615F9BF7")
          signatures.map(_.data.toIndexedSeq).map(bytesToHex(_)) mustEqual Seq("7C7D2D5CB412C0E8CC0C6776CC" +
            "BE0FA1366D699300C9A784BA05A9FD857E84403178C2F415B5E99425391FB0587E4F48A9F3DB642903163880C909BC7F0B0B04")
          bytesToHex(txn.hash) mustEqual "7D91CFC50E907A677F769E6DB82BDB958D148D810955C597AA7A4B24AE97475B"
          feeBump must beEmpty
      }
    }
  }
}

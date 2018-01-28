package stellar.scala.sdk

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.TransactionEnvelope
import stellar.scala.sdk.op.{CreateAccountOperation, Operation}

import scala.util.Try

class TransactionSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a transaction fee" should {
    "be equal to 100 * the quantity of operations" >> prop { (source: Account, ops: Seq[Operation]) =>
      Transaction(source, NoMemo, ops).fee mustEqual ops.size * 100
    }.setGen2(Gen.nonEmptyListOf(genOperation))
  }

  "a transaction" should {
    "allow adding of operations one at a time" >> prop { (source: Account, ops: Seq[Operation]) =>
      val expected = Transaction(source, NoMemo, ops)
      val actual = ops.foldLeft(Transaction(source, NoMemo)) { case (txn, op) => txn add op }
      actual must beEquivalentTo(expected)
    }

    "allow signing of the transaction one signature at a time" >> prop { (transaction: Transaction, signers: Seq[KeyPair]) =>
      val expected = transaction.sign(signers.head, signers.tail: _*).get
      val actual: SignedTransaction = signers match {
        case Seq(only) => transaction.sign(only).get
        case h +: t => t.foldLeft(transaction.sign(h).get) { case (txn, kp) => txn.sign(kp).get}
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
        operations = Seq(CreateAccountOperation(dest, Amount.lumens(2000)))
      )(TestNetwork).sign(source)

      txn.flatMap(_.toEnvelopeXDRBase64) must beSuccessfulTry("AAAAAF7FIiDToW1fOYUFBC0dmyufJbFTOa2GQESGz+S2h5ViAAAAZAAKVaMAAAABAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAA7eBSYbzcL5UKo7oXO24y1ckX+XuCtkDsyNHOp1n1bxAAAAAEqBfIAAAAAAAAAAABtoeVYgAAAEDLki9Oi700N60Lo8gUmEFHbKvYG4QSqXiLIt9T0ru2O5BphVl/jR9tYtHAD+UeDYhgXNgwUxqTEu1WukvEyYcD")
      txn.map(_.transaction.source) must beSuccessfulTry[Account].like{ case accn => accn must beEquivalentTo(account)}
      txn.map(_.transaction.fee) must beSuccessfulTry(100)
    }
  }

  "signing a transaction" should {
    "add a signature to that transaction" >> prop { (source: Account, op: Operation, signers: Seq[KeyPair]) =>
      val signatures = Transaction(source, NoMemo, Seq(op)).sign(signers.head, signers.tail: _*).get.signatures
      signatures must haveSize(signers.length)
    }.setGen3(Gen.nonEmptyListOf(genKeyPair))
  }

  "a signed transaction" should {
    "serialise to xdr" >> prop { (t: Transaction, signer: KeyPair) =>
      t.sign(signer).map(_.toEnvelopeXDR) must beSuccessfulTry[TransactionEnvelope]
    }
  }

  "an unsigned transaction" should {
    "serialise to xdr" >> prop { txn: Transaction =>
      val xdr = txn.toXDR
      xdr.getExt.getDiscriminant mustEqual 0
      xdr.getFee.getUint32 mustEqual txn.fee
      xdr.getSeqNum.getSequenceNumber.getUint64 mustEqual txn.source.sequenceNumber
      xdr.getSourceAccount.getAccountID must beEquivalentTo(txn.source.keyPair.getXDRPublicKey)
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
  }
}

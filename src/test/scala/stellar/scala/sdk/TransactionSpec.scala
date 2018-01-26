package stellar.scala.sdk

import org.scalacheck.Gen
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr
import org.stellar.sdk.xdr.TransactionEnvelope
import stellar.scala.sdk.op.Operation

import scala.util.Success

class TransactionSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a transaction fee" should {
    "be equal to 100 * the quantity of operations" >> prop { (source: Account, ops: Seq[Operation]) =>
      Transaction(source, NoMemo, ops).fee mustEqual ops.size * 100
    }.setGen2(Gen.nonEmptyListOf(genOperation))
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
      // xdr.getOperations.map(Operation.fromXDR).head mustEqual Success(txn.operations.last) // todo --- wip
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
    }
  }

}

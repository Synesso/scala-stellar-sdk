package stellar.scala.sdk

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.Signature
import stellar.scala.sdk.op.Operation

class TransactionSpec extends Specification with ArbitraryInput {

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
    "serialise to xdr" >> prop { (t: Transaction, s: Signature) =>
      pending
//      t.Signed(Seq(s))
    }
  }

}

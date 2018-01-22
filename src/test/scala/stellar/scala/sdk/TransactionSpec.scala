package stellar.scala.sdk

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.scala.sdk.op.Operation

class TransactionSpec extends Specification with ArbitraryInput {

  implicit val network = TestNetwork

  "a transaction fee" should {
    "be equal to 100 * the quantity of operations" >> prop { (source: Account, ops: Seq[Operation]) =>
      Transaction(source, NoMemo, ops).fee mustEqual ops.size * 100
    }.setGen2(Gen.nonEmptyListOf(genOperation))
  }

  "signing a transaction" should {
    "add a signature to that transaction" >> prop { (source: Account, op: Operation, signers: Seq[KeyPair]) =>
      pending // todo
      val signatures = Transaction(source, NoMemo, Seq(op)).sign(signers.head, signers.tail: _*).signatures
      signatures must haveSize(signers.length)
    }.setGen3(Gen.nonEmptyListOf(genKeyPair))
  }

}

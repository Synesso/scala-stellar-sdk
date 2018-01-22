package stellar.scala.sdk

import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.scala.sdk.op.Operation

class TransactionSpec extends Specification with ArbitraryInput {

  "a transaction fee" should {
    "be equal to 100 * the quantity of operations" >> prop { ops: Seq[Operation] =>
      Transaction(ops.head, ops.tail: _*).fee mustEqual ops.size * 100
    }.setGen(Gen.nonEmptyListOf(genOperation))
  }

}

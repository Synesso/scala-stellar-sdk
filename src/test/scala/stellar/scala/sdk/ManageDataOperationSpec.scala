package stellar.scala.sdk

import org.scalacheck.Gen
import org.specs2.mutable.Specification

class ManageDataOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "a write data operation" should {
    "serde via xdr" >> prop { (source: KeyPair, name: String, value: String) =>
      val input = WriteDataOperation(name, value.getBytes, Some(source))
      val triedOperation = Operation.fromXDR(input.toXDR)
      if (triedOperation.isFailure) throw triedOperation.failed.get
      triedOperation must beSuccessfulTry.like {
        case wdo: WriteDataOperation =>
          wdo.name mustEqual name
          new String(wdo.value) mustEqual value
          wdo.sourceAccount must beNone
      }
    }.setGen2(Gen.identifier)
  }

  "a delete data operation" should {
    "serde via xdr" >> prop { (source: KeyPair, name: String) =>
      val input = DeleteDataOperation(name, Some(source))
      val triedOperation = Operation.fromXDR(input.toXDR)
      if (triedOperation.isFailure) throw triedOperation.failed.get
      triedOperation must beSuccessfulTry.like {
        case ddo: DeleteDataOperation =>
          ddo.name mustEqual name
          ddo.sourceAccount must beNone
      }
    }.setGen2(Gen.identifier)
  }

}

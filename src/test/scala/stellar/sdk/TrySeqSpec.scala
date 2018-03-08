package stellar.sdk

import org.specs2.mutable.Specification
import stellar.sdk.TrySeq._

import scala.util.{Failure, Success}

class TrySeqSpec extends Specification {

  "sequencing an empty list of trys" should {
    "be a successful empty list" >> {
      sequence(Nil) mustEqual Success(Nil)
    }
  }

  "sequencing a list of successes" should {
    "be a successful list" >> {
      sequence(Seq(Success(1), Success(2), Success(3))) must beSuccessfulTry(Seq(1, 2, 3))
    }
  }

  "sequencing a list with failures" should {
    "be the first failure" >> {
      sequence(Seq(
        Success(1),
        Success(2),
        Failure(new RuntimeException("3")),
        Success(4),
        Failure(new RuntimeException("5")))) must beFailedTry.like {
        case e: RuntimeException =>
          e.getMessage mustEqual "3"
      }
    }
  }

}

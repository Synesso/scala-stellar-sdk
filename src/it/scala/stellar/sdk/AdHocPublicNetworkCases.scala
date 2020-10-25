package stellar.sdk

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.{Memo, MemoText}

import scala.concurrent.duration._
class AdHocPublicNetworkCases(implicit val ee: ExecutionEnv) extends Specification {

  private implicit val network: Network = PublicNetwork

  "memo text with unusual encoding" should {
    "be parsed" >> {
      PublicNetwork.transaction("424ac176edc448b3e87db0eae61ba621f5f7b5217c10b6016f74d85cdcaafb0a")
        .map(_.memo) must beLikeA[Memo]({ case m: MemoText =>
        m.text mustEqual "I�T��@�v\u0018[��:�\u0011)�\u0014jZ"
      }).awaitFor(5.seconds)
    }
  }

}

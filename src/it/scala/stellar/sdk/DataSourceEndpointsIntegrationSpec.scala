package stellar.sdk

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.resp.TransactionHistoryResp
import scala.concurrent.duration._

import scala.concurrent.Future

class DataSourceEndpointsIntegrationSpec(implicit ee: ExecutionEnv) extends Specification {

  implicit val system = ActorSystem("local-network-integration-spec")
  implicit val materializer = ActorMaterializer()

  "transaction source" should {
    "provide all future transactions" >> {
      val results: Future[Seq[TransactionHistoryResp]] = PublicNetwork.transactionSource().take(10)
        .runWith(Sink.seq[TransactionHistoryResp])
      results.isCompleted must beFalse
      results.map(_.size) must beEqualTo(10).awaitFor(1 minute)
    }
  }

}

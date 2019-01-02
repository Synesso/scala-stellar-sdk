package stellar.sdk

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.Record
import stellar.sdk.model.op.{Operation, PayOperation, Transacted}
import stellar.sdk.model.result.TransactionHistory
import stellar.sdk.model.response.EffectResponse

import scala.concurrent.Future
import scala.concurrent.duration._

class DataSourceEndpointsIntegrationSpec(implicit ee: ExecutionEnv) extends Specification {

  implicit val system = ActorSystem("local-network-integration-spec")
  implicit val materializer = ActorMaterializer()

  "transaction source" should {
    "provide all future transactions" >> {
      val results: Future[Seq[TransactionHistory]] = PublicNetwork.transactionSource().take(5)
        .runWith(Sink.seq[TransactionHistory])
      results.map(_.size) must beEqualTo(5).awaitFor(1 minute)
    }
    "provide transactions history" >> {
      val results: Future[Seq[TransactionHistory]] = PublicNetwork.transactionSource(Record(100)).take(5)
        .runWith(Sink.seq[TransactionHistory])
      results.map(_.size) must beEqualTo(5).awaitFor(1 minute)
    }
  }

  "payment operation source" should {
    "provide all future payment operations" >> {
      val results = PublicNetwork.paymentsSource().take(3).runWith(Sink.seq[Transacted[PayOperation]])
      results.map(_.size) must beEqualTo(3).awaitFor(1 minute)
    }
    "provide pay operation history" >> {
      val results = PublicNetwork.paymentsSource(Record(300)).take(10).runWith(Sink.seq[Transacted[PayOperation]])
      results.map(_.size) must beEqualTo(10).awaitFor(1 minute)
    }
  }

  "operation source" should {
    "provide all future operations" >> {
      val results = PublicNetwork.operationsSource().take(3).runWith(Sink.seq[Transacted[Operation]])
      results.map(_.size) must beEqualTo(3).awaitFor(1 minute)
    }
    "provide operation history" >> {
      val results = PublicNetwork.operationsSource(Record(900)).take(10).runWith(Sink.seq[Transacted[Operation]])
      results.map(_.size) must beEqualTo(10).awaitFor(1 minute)
    }
  }

  "event source" should {
    "provide all future events" >> {
      val results = PublicNetwork.effectsSource().take(3).runWith(Sink.seq[EffectResponse])
      results.map(_.size) must beEqualTo(3).awaitFor(1 minute)
    }
    "provide event history" >> {
      val results = PublicNetwork.effectsSource(Record(1400)).take(15).runWith(Sink.seq[EffectResponse])
      results.map(_.size) must beEqualTo(15).awaitFor(1 minute)
    }
  }

}

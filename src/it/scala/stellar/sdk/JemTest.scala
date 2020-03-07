package stellar.sdk

import org.specs2.mutable.Specification
import java.io.EOFException
import java.time.{Instant, Period}

import com.typesafe.scalalogging.LazyLogging
import okhttp3.HttpUrl
import org.json4s.JsonDSL._
import org.specs2.concurrent.ExecutionEnv
import stellar.sdk.inet.HorizonEntityNotFound
import stellar.sdk.model.Amount.lumens
import stellar.sdk.model.TimeBounds.Unbounded
import stellar.sdk.model.TradeAggregation.FifteenMinutes
import stellar.sdk.model._
import stellar.sdk.model.op._
import stellar.sdk.model.response._
import stellar.sdk.model.result.TransactionHistory
import stellar.sdk.util.ByteArrays

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import stellar.sdk._
class JemTest(implicit ee: ExecutionEnv) extends Specification with DomainMatchersIT with LazyLogging {

  def await[T](f: Future[T]): T = Await.result[T](f, 5.seconds)
  implicit val network = StandaloneNetwork(HttpUrl.parse(s"http://localhost:8000"))

  val masterAccountKey = network.masterAccount
  val masterAccount = await(network.account(masterAccountKey).map(_.toAccount))

  val accnA = KeyPair.fromPassphrase("account a")
  val accnB = KeyPair.fromPassphrase("account b")
  val accnC = KeyPair.fromPassphrase("account c")
  val accnD = KeyPair.fromPassphrase("account d")

  val accounts = Set(accnA, accnB, accnC, accnD)

  val aardvarkA = Asset("Aardvark", accnA)
  val beaverA = Asset("Beaver", accnA)
  val chinchillaA = Asset("Chinchilla", accnA)
  val chinchillaMaster = Asset("Chinchilla", masterAccountKey)
  val dachshundB = Asset("Dachshund", accnB)
  val txnHash2 = "e13447898b27dbf278d4411022e2e6d0aae78ef70670c7af7834a1f2a6d191d8"
  val txnHash3 = "2ee32cbbe5f2dceca2934d4f1fa8e41c6661e5270b952fd6a7170ecb314ca0c8"

  "asset endpoint" should {
    "list all assets" >> {
      val eventualResps = network.assets().map(_.toSeq)
      eventualResps must containTheSameElementsAs(Seq(
        AssetResponse(aardvarkA, 0, 0, authRequired = true, authRevocable = true),
        AssetResponse(beaverA, 0, 0, authRequired = true, authRevocable = true),
        AssetResponse(chinchillaA, 101, 1, authRequired = true, authRevocable = true),
        AssetResponse(chinchillaMaster, 101, 1, authRequired = false, authRevocable = false),
        AssetResponse(dachshundB, 0, 0, authRequired = false, authRevocable = false)
      )).awaitFor(10 seconds)
    }

    "filter assets by code" >> {
      val byCode = network.assets(code = Some("Chinchilla"))
      byCode.map(_.size) must beEqualTo(2).awaitFor(10 seconds)
      byCode.map(_.map(_.asset.code).toSet) must beEqualTo(Set("Chinchilla")).awaitFor(10 seconds)
    }

    "filter assets by issuer" >> {
      val byIssuer = network.assets(issuer = Some(accnA)).map(_.take(10).toList)
      byIssuer.map(_.size) must beEqualTo(3).awaitFor(10 seconds)
      byIssuer.map(_.map(_.asset.issuer.accountId).distinct) must beEqualTo(Seq(accnA.accountId)).awaitFor(10 seconds)
    }

    "filter assets by code and issuer" >> {
      network.assets(code = Some("Chinchilla"), issuer = Some(accnA)).map(_.toList)
        .map(_.map(_.asset)) must beLike[Seq[NonNativeAsset]] {
        case Seq(only) => only must beEquivalentTo(IssuedAsset12("Chinchilla", accnA))
      }.awaitFor(10 seconds)
    }
  }


}

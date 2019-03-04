package stellar.sdk

import org.json4s.JObject
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import stellar.sdk.model._
import stellar.sdk.model.response.FederationResponse

import scala.annotation.tailrec
import scala.concurrent.duration._

class FederationServerSpec(implicit ec: ExecutionEnv) extends Specification with Mockito with ArbitraryInput with AfterAll {

  val server = new StubServer()
  override def afterAll(): Unit = server.stop()

  private def toJson(fr: FederationResponse, withAddress: Boolean = true): String = {

    val memo: Option[JObject] = fr.memo match {
      case NoMemo => None
      case MemoId(id) => Some(("memo_type" -> "id") ~ ("memo" -> id.toString))
      case MemoText(txt) => Some(("memo_type" -> "text") ~ ("memo" -> txt))
      case m: MemoWithHash => Some(("memo_type" -> "hash") ~ ("memo" -> m.hex))
    }

    val address: Option[JObject] = if (withAddress) Some("stellar_address" -> fr.address) else None

    val doc: JObject = (address.toSeq ++ memo.toSeq).foldLeft[JObject]("account_id" -> fr.account.accountId)(_ ~ _)

    compact(render(doc))
  }

  @tailrec
  private def sample: FederationResponse = {
    genFederationResponse.sample match {
      case Some(r) => r
      case None => sample
    }
  }

  "federation server" should {
    "return response by name" >> {
      val fr = sample
      server.expectGet("fed.json", Map("q" -> fr.address, "type" -> "name"), toJson(fr))
      FederationServer(s"http://localhost:8002/fed.json").byName(fr.address) must beSome(fr).awaitFor(10.seconds)
    }

    "return response by name when server does not provide the address" >> {
      val fr = sample
      server.expectGet("fed.json", Map("q" -> fr.address, "type" -> "name"), toJson(fr, withAddress = false))
      FederationServer(s"http://localhost:8002/fed.json").byName(fr.address) must beSome(fr).awaitFor(10.seconds)
    }

    "return nothing when the account does not exist" >> {
      val name = Gen.identifier.sample.get
      server.forgetGet("fed.json", Map("q" -> name, "type" -> "name"))
      FederationServer(s"http://localhost:8002/fed.json").byName(name) must beNone.awaitFor(10.seconds)
    }

    "handle redirect" >> {
      val fr = sample
      server.expectGetRedirect("http://localhost:8002", "federation", "fed.json",
        Map("q" -> fr.address, "type" -> "name"), toJson(fr))
      FederationServer("http://localhost:8002/fed.json").byName(fr.address) must beSome(fr).awaitFor(10.seconds)
    }
  }

}

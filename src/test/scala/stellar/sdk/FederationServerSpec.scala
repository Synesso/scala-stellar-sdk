package stellar.sdk

import org.json4s.JObject
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.scalacheck.Gen
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

  private def toJson(fr: FederationResponse, suppress: Option[String] = None): String = {

    val memo: Option[JObject] = fr.memo match {
      case NoMemo => None
      case MemoId(id) => Some(("memo_type" -> "id") ~ ("memo" -> id.toString))
      case MemoText(txt) => Some(("memo_type" -> "text") ~ ("memo" -> txt))
      case m: MemoWithHash => Some(("memo_type" -> "hash") ~ ("memo" -> m.hex))
    }

    val address: Option[JObject] = if (suppress.contains("stellar_address")) None else Some("stellar_address" -> fr.address)
    val account: Option[JObject] = if (suppress.contains("account_id")) None else Some("account_id" -> fr.account.accountId)

    val doc: JObject = (address.toSeq ++ memo.toSeq ++ account.toSeq).reduceLeft[JObject](_ ~ _)

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

    "return response by name when server does not echo the address" >> {
      val fr = sample
      server.expectGet("fed.json", Map("q" -> fr.address, "type" -> "name"), toJson(fr, suppress = Some("stellar_address")))
      FederationServer(s"http://localhost:8002/fed.json").byName(fr.address) must beSome(fr).awaitFor(10.seconds)
    }

    "return nothing when the address does not exist" >> {
      val name = Gen.identifier.sample.get
      server.forgetGet("fed.json", Map("q" -> name, "type" -> "name"))
      FederationServer(s"http://localhost:8002/fed.json").byName(name) must beNone.awaitFor(10.seconds)
    }

    "return response by name via redirect" >> {
      val fr = sample
      server.expectGetRedirect("http://localhost:8002", "federation", "fed.json",
        Map("q" -> fr.address, "type" -> "name"), toJson(fr))
      FederationServer("http://localhost:8002/fed.json").byName(fr.address) must beSome(fr).awaitFor(10.seconds)
    }

    "handle server error when fetching response by name" >> {
      pending
    }

    "handle invalid document response when fetching response by name" >> {
      pending
    }

    "return response by account" >> {
      val fr = sample
      server.expectGet("fed.json", Map("q" -> fr.account.accountId, "type" -> "id"), toJson(fr))
      FederationServer(s"http://localhost:8002/fed.json").byAccount(fr.account) must beSome(fr).awaitFor(10.seconds)
    }

    "return response by account when server does not echo the account" >> {
      val fr = sample
      server.expectGet("fed.json", Map("q" -> fr.account.accountId, "type" -> "id"), toJson(fr, suppress = Some("account_id")))
      FederationServer(s"http://localhost:8002/fed.json").byAccount(fr.account) must beSome(fr).awaitFor(10.seconds)
    }

    "return nothing with the account does not exist" >> {
      val account = genPublicKey.sample.get
      server.forgetGet("fed.json", Map("q" -> account.accountId, "type" -> "id"))
      FederationServer(s"http://localhost:8002/fed.json").byAccount(account) must beNone.awaitFor(10.seconds)
    }

    "return response by account via redirect" >> {
      val fr = sample
      server.expectGetRedirect("http://localhost:8002", "federation", "fed.json",
        Map("q" -> fr.account.accountId, "type" -> "id"), toJson(fr))
      FederationServer("http://localhost:8002/fed.json").byAccount(fr.account) must beSome(fr).awaitFor(10.seconds)
    }
  }

}

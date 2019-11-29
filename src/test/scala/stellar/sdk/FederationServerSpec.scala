package stellar.sdk

import okhttp3.mockwebserver.{MockResponse, MockWebServer, RecordedRequest}
import org.json4s.JObject
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.scalacheck.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.inet.RestException
import stellar.sdk.model._
import stellar.sdk.model.response.FederationResponse

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class FederationServerSpec(implicit ec: ExecutionEnv) extends Specification with ArbitraryInput {

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
      val (resp, req) = reqResp(Some(toJson(fr)), _.byName(fr.address))
      resp must beSome(fr)
      req must beRequestLike("/fed.json", Map("type" -> "name", "q" -> fr.address))
    }

    "return response by name when server does not echo the address" >> {
      val fr = sample
      val (resp, req) = reqResp(Some(toJson(fr, suppress = Some("stellar_address"))), _.byName(fr.address))
      resp must beSome(fr)
      req must beRequestLike("/fed.json", Map("type" -> "name", "q" -> fr.address))
    }

    "return nothing when the address does not exist" >> {
      val name = Gen.identifier.sample.get
      val (resp, req) = reqResp(None, _.byName(name))
      resp must beNone
      req must beRequestLike("/fed.json", Map("type" -> "name", "q" -> name))
    }

    "return response by name via redirect" >> {
      val fr = sample
      val (resp, req) = reqRespRedirect(None, _.byName(fr.address))
      resp must beSome(fr)
      req must beRequestLike("/fed.json", Map("type" -> "name", "q" -> fr.address))
    }

    "handle server error when fetching response by name" >> {
      val name = Gen.identifier.sample.get
      reqRespError(_.byName(name)) must throwA[RestException]
    }

    "handle invalid document response when fetching response by name" >> {
      val name = Gen.identifier.sample.get
      reqResp(Some("""{"something":"else"}"""), _.byName(name)) must throwA[RestException]
    }

    "handle invalid non-document response when fetching response by name" >> {
      val name = Gen.identifier.sample.get
      reqResp(Some("hobnobs"), _.byName(name)) must throwA[RestException]
    }

    "return response by account" >> {
      val fr = sample
      val (resp, req) = reqResp(Some(toJson(fr)), _.byAccount(fr.account))
      resp must beSome(fr)
      req must beRequestLike("/fed.json", Map("type" -> "id", "q" -> fr.account.accountId))
    }

    "return response by account when server does not echo the account" >> {
      val fr = sample
      val (resp, req) = reqResp(Some(toJson(fr, suppress = Some("account_id"))), _.byAccount(fr.account))
      resp must beSome(fr)
      req must beRequestLike("/fed.json", Map("type" -> "id", "q" -> fr.account.accountId))
    }

    "return nothing when the account does not exist" >> {
      val fr = sample
      val (resp, req) = reqResp(None, _.byAccount(fr.account))
      resp must beNone
      req must beRequestLike("/fed.json", Map("type" -> "id", "q" -> fr.account.accountId))
    }

    "return response by account via redirect" >> {
      val fr = sample
      val (resp, req) = reqRespRedirect(None, _.byAccount(fr.account))
      resp must beSome(fr)
      req must beRequestLike("/fed.json", Map("type" -> "id", "q" -> fr.account.accountId))
    }

    "handle server error when fetching response by account" >> {
      val account = genPublicKey.sample.get
      reqRespError(_.byAccount(account)) must throwA[RestException]
    }

    "handle invalid document response when fetching response by account" >> {
      val account = genPublicKey.sample.get
      reqResp(Some("""{"something":"else"}"""), _.byAccount(account)) must throwA[RestException]
    }

    "handle invalid non-document response when fetching response by account" >> {
      val account = genPublicKey.sample.get
      reqResp(Some("dachshunds"), _.byAccount(account)) must throwA[RestException]
    }
  }

  private def beRequestLike(path: String, headers: Map[String, String]) = {
    beLike[RecordedRequest] { case request =>
      request.getPath mustEqual path
      forall(headers) { case (k, v) =>
        request.getHeaders.get(k) mustEqual v
      }
    }
  }

  private def reqResp(json: Option[String], method: FederationServer => Future[Option[FederationResponse]])
  : (Option[FederationResponse], RecordedRequest) = {
    val server = new MockWebServer
    exec(server, json, method) -> server.takeRequest()
  }

  private def reqRespRedirect(json: Option[String],
                              method: FederationServer => Future[Option[FederationResponse]])
  : (Option[FederationResponse], RecordedRequest) = {

    val server = new MockWebServer
    server.enqueue(new MockResponse().setResponseCode(301).setHeader("Location", "fed2.json"))
    exec(server, json, method) -> {
      server.takeRequest(); // dropped redirect
      server.takeRequest()  // actual
    }
  }

  private def reqRespError(method: FederationServer => Future[Option[FederationResponse]])
  : (Option[FederationResponse], RecordedRequest) = {
    val server = new MockWebServer
    server.enqueue(new MockResponse().setResponseCode(500))
    exec(server, None, method) -> server.takeRequest()
  }

  private def exec(server: MockWebServer,
                   json: Option[String],
                   method: FederationServer => Future[Option[FederationResponse]])
  : Option[FederationResponse] = {
    json.foreach(s => server.enqueue(new MockResponse().setBody(s)))
    server.start()
    val federationServer = FederationServer(server.url("/fed.json"))
    val result = Await.result(method(federationServer), 3.seconds)
    server.shutdown()
    result
  }
}

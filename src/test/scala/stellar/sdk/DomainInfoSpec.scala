package stellar.sdk

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import stellar.sdk.StubServer.ReplyWithText
import stellar.sdk.inet.RestException

import scala.concurrent.Future
import scala.concurrent.duration._

class DomainInfoSpec(implicit ee: ExecutionEnv) extends Specification with AfterAll {

  sequential

  val server = new StubServer()

  override def afterAll(): Unit = server.stop()

  "domain info TOML lookup" should {
    "find federation server" >> {
      roundTripDomainInfo("""FEDERATION_SERVER="https://xyz.com/fedsrv"""")
        .map(_.federationServer) must beSome(FederationServer(Uri("https://xyz.com"), Path("/fedsrv")))
        .awaitFor(5.seconds)
    }

    "find auth server" >> {
      roundTripDomainInfo("""AUTH_SERVER="https://xyz123.com/authsrv"""")
        .map(_.authServer) must beSome(Uri("https://xyz123.com/authsrv"))
        .awaitFor(5.seconds)
    }

    "find transfer server" >> {
      roundTripDomainInfo("""TRANSFER_SERVER="https://abc123.com/tfrsrv"""")
        .map(_.transferServer) must beSome(Uri("https://abc123.com/tfrsrv"))
        .awaitFor(5.seconds)
    }

    "find kyc server" >> {
      roundTripDomainInfo("""KYC_SERVER="https://kyc.org/a/b/"""")
        .map(_.kycServer) must beSome(Uri("https://kyc.org/a/b/"))
        .awaitFor(5.seconds)
    }

    "find web auth endpoint" >> {
      roundTripDomainInfo("""WEB_AUTH_ENDPOINT="https://charlottes.web/"""")
        .map(_.webAuthEndpoint) must beSome(Uri("https://charlottes.web/"))
        .awaitFor(5.seconds)
    }

    "find public horizon url" >> {
      roundTripDomainInfo("""HORIZON_URL="https://event.horizon/"""")
        .map(_.horizonEndpoint) must beSome(Uri("https://event.horizon/"))
        .awaitFor(5.seconds)
    }

    "find signer key" >> {
      val kp = KeyPair.random
      roundTripDomainInfo(s"""SIGNER_KEY="${kp.accountId}"""")
        .map(_.signerKey) must beSome(kp.asPublicKey)
        .awaitFor(5.seconds)
    }

    "find uri request signing key" >> {
      val kp = KeyPair.random
      roundTripDomainInfo(s"""URI_REQUEST_SIGNING_KEY="${kp.accountId}"""")
        .map(_.uriRequestSigningKey) must beSome(kp.asPublicKey)
        .awaitFor(5.seconds)
    }

    "find uri request signing key" >> {
      val kp = KeyPair.random
      roundTripDomainInfo(s"""URI_REQUEST_SIGNING_KEY="${kp.accountId}"""")
        .map(_.uriRequestSigningKey) must beSome(kp.asPublicKey)
        .awaitFor(5.seconds)
    }

    "handle malformed signer key" >> {
      val kp = KeyPair.random
      roundTripDomainInfo(s"""SIGNER_KEY="${kp.accountId.drop(2)}"""")
        .map(_.signerKey) must throwA[RestException]
        .awaitFor(5.seconds)
    }

    "find version" >> {
      roundTripDomainInfo(s"""VERSION="ponies"""")
        .map(_.version) must beSome("ponies")
        .awaitFor(5.seconds)
    }

    "find empty list of accounts" >> {
      roundTripDomainInfo(s"""ACCOUNTS=[]""")
        .map(_.accounts) must beEqualTo(List.empty[PublicKey])
        .awaitFor(5.seconds)
    }

    "find single account" >> {
      val pk = KeyPair.random.asPublicKey
      roundTripDomainInfo(s"""ACCOUNTS=["${pk.accountId}"]""")
        .map(_.accounts) must contain(pk)
        .awaitFor(5.seconds)
    }

    "find multiple accounts" >> {
      val pks = List.fill(10){KeyPair.random.asPublicKey}
      roundTripDomainInfo(s"""ACCOUNTS=["${pks.map(_.accountId).mkString("\",\"")}"]""")
        .map(_.accounts) must containTheSameElementsAs(pks)
        .awaitFor(5.seconds)
    }

    "handle a malformed account" >> {
      val pks = List.fill(10){KeyPair.random.asPublicKey}
      val value = pks.map(_.accountId).zipWithIndex.map {
        case (accnId , i) if i == 7 => accnId.drop(1)
        case (accnId , _)           => accnId
      }.mkString("\",\"")
      roundTripDomainInfo(s"""ACCOUNTS=["$value"]""")
        .map(_.accounts) must throwA[RestException]
        .awaitFor(5.seconds)
    }

  }

  private def roundTripDomainInfo(content: String): Future[DomainInfo] = {
    server.expectGet(".well-known/stellar.toml", Map.empty, ReplyWithText(content))
    DomainInfo.forDomain("http://localhost:8002").map(_.get)
  }

  "parsing toml" should {
    "be successful when endpoint is correctly defined" >> {
      DomainInfo.from("""FEDERATION_SERVER="https://fruitcakes.com/federation.pl"""") mustEqual
        DomainInfo(federationServer = Some(FederationServer("https://fruitcakes.com/federation.pl")))
    }

    "be successful when there is other content in the file" >> {
      DomainInfo.from(
        """
          |# this is how we roll
          |ROLL="like this"
          |NUMBERS = [1, 2, 3]
          |FEDERATION_SERVER="bonanza"
          |[section_9]
          |NUMBERS = [[66, 22],[33,99]]
          |FEDERATION_SERVER="ignoreme"
        """.stripMargin) mustEqual
        DomainInfo(federationServer = Some(FederationServer("bonanza")))
    }

    "not fail when the document is empty" >> {
      DomainInfo.from("") mustEqual DomainInfo()
    }

    "fail when the document is something other than TOML" >> {
      DomainInfo.from("""{'pandas': 12}""") must throwA[DomainInfoParseException]
    }

    "fail when a value is defined as the wrong type" >> {
      DomainInfo.from("""FEDERATION_SERVER=17""") must throwA[DomainInfoParseException]
    }

    "not fail when the endpoint is not defined" >> {
      DomainInfo.from("""THANKS_FOR_THE_FISH=42""") mustEqual DomainInfo()
    }
  }

}

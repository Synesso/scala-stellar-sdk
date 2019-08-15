package stellar.sdk

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import stellar.sdk.StubServer.ReplyWithText
import stellar.sdk.inet.RestException
import stellar.sdk.model.domain.{DomainInfo, DomainInfoParseException}

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

  "Documentation TOML parsing" should {
    def doc(k: String, v: String) =
      s"""[DOCUMENTATION]
         |$k=$v
        """.stripMargin

    "find organisation name" >> {
      roundTripDomainInfo(doc("ORG_NAME", """"SausageDog"""")).map(_.issuerDocumentation.flatMap(_.name)) must
        beSome("SausageDog").awaitFor(5.seconds)
    }

    "find 'doing business as' name" >> {
      roundTripDomainInfo(doc("ORG_DBA", """"Border Collie"""")).map(_.issuerDocumentation.flatMap(_.doingBusinessAs)) must
        beSome("Border Collie").awaitFor(5.seconds)
    }

    "find url" >> {
      roundTripDomainInfo(doc("ORG_URL", """"https://puppies.com/"""")).map(
        _.issuerDocumentation.flatMap(_.url)) must
        beSome(Uri("https://puppies.com/")).awaitFor(5.seconds)
    }

    "find logo" >> {
      roundTripDomainInfo(doc("ORG_LOGO", """"https://puppies.com/le_puppy.png"""")).map(
        _.issuerDocumentation.flatMap(_.logo)) must
        beSome(Uri("https://puppies.com/le_puppy.png")).awaitFor(5.seconds)
    }

    "find description" >> {
      roundTripDomainInfo(doc("ORG_DESCRIPTION", """"a place for humans to love dogs""""))
        .map(_.issuerDocumentation.flatMap(_.description)) must
        beSome("a place for humans to love dogs").awaitFor(5.seconds)
      }

    "find physical address" >> {
      roundTripDomainInfo(doc("ORG_PHYSICAL_ADDRESS", """"123 Woof Lane"""")).map(_.issuerDocumentation
        .flatMap(_.physicalAddress)) must beSome("123 Woof Lane").awaitFor(5.seconds)
    }

    "find physical address attestation" >> {
      roundTripDomainInfo(doc("ORG_PHYSICAL_ADDRESS_ATTESTATION", """"https://puppies.com/address.pdf"""")).map(
        _.issuerDocumentation.flatMap(_.physicalAddressAttestation)) must
        beSome(Uri("https://puppies.com/address.pdf")).awaitFor(5.seconds)
    }

    "find phone number" >> {
      roundTripDomainInfo(doc("ORG_PHONE_NUMBER", """"1800-woof-woof"""")).map(_.issuerDocumentation
        .flatMap(_.phoneNumber)) must beSome("1800-woof-woof").awaitFor(5.seconds)
    }

    "find phone number attestation" >> {
      roundTripDomainInfo(doc("ORG_PHONE_NUMBER_ATTESTATION", """"https://puppies.com/phone.pdf"""")).map(
        _.issuerDocumentation.flatMap(_.phoneNumberAttestation)) must
        beSome(Uri("https://puppies.com/phone.pdf")).awaitFor(5.seconds)
    }

    "find keybase account name" >> {
      roundTripDomainInfo(doc("ORG_KEYBASE", """"puppi3s"""")).map(_.issuerDocumentation
        .flatMap(_.keybase)) must beSome("puppi3s").awaitFor(5.seconds)
    }

    "find twitter handle" >> {
      roundTripDomainInfo(doc("ORG_TWITTER", """"le_puppies"""")).map(_.issuerDocumentation
        .flatMap(_.twitter)) must beSome("le_puppies").awaitFor(5.seconds)
    }

    "find github account name" >> {
      roundTripDomainInfo(doc("ORG_GITHUB", """"püpp33s"""")).map(_.issuerDocumentation
        .flatMap(_.github)) must beSome("püpp33s").awaitFor(5.seconds)
    }

    "find official email" >> {
      roundTripDomainInfo(doc("ORG_OFFICIAL_EMAIL", """"puppies@woof.com"""")).map(_.issuerDocumentation
        .flatMap(_.email)) must beSome("puppies@woof.com").awaitFor(5.seconds)
    }

    "find licensing authority" >> {
      roundTripDomainInfo(doc("ORG_LICENSING_AUTHORITY", """"RSPCA"""")).map(_.issuerDocumentation
        .flatMap(_.licensingAuthority)) must beSome("RSPCA").awaitFor(5.seconds)
    }

    "find license type" >> {
      roundTripDomainInfo(doc("ORG_LICENSE_TYPE", """"BARK-BARK"""")).map(_.issuerDocumentation
        .flatMap(_.licenseType)) must beSome("BARK-BARK").awaitFor(5.seconds)
    }

    "find license number" >> {
      roundTripDomainInfo(doc("ORG_LICENSE_NUMBER", """"7-Zark-7"""")).map(_.issuerDocumentation
        .flatMap(_.licenseNumber)) must beSome("7-Zark-7").awaitFor(5.seconds)
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

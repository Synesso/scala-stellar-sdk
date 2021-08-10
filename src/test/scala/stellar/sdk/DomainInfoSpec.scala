package stellar.sdk

import okhttp3.HttpUrl
import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.scalacheck.Arbitrary
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.scalacheck.Parameters
import stellar.sdk.inet.RestException
import stellar.sdk.model.domain._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class DomainInfoSpec(implicit ee: ExecutionEnv) extends Specification with DomainInfoGenerators {

  sequential

  "domain info TOML lookup" should {
    "find federation server" >> {
      roundTripDomainInfo("""FEDERATION_SERVER="https://xyz.com/fedsrv"""")
        .map(_.federationServer) must beSome(FederationServer(HttpUrl.parse("https://xyz.com/fedsrv")))
        .awaitFor(5.seconds)
    }

    "find auth server" >> {
      roundTripDomainInfo("""AUTH_SERVER="https://xyz123.com/authsrv"""")
        .map(_.authServer) must beSome(HttpUrl.parse("https://xyz123.com/authsrv"))
        .awaitFor(5.seconds)
    }

    "find transfer server" >> {
      roundTripDomainInfo("""TRANSFER_SERVER="https://abc123.com/tfrsrv"""")
        .map(_.transferServer) must beSome(HttpUrl.parse("https://abc123.com/tfrsrv"))
        .awaitFor(5.seconds)
    }

    "find kyc server" >> {
      roundTripDomainInfo("""KYC_SERVER="https://kyc.org/a/b/"""")
        .map(_.kycServer) must beSome(HttpUrl.parse("https://kyc.org/a/b/"))
        .awaitFor(5.seconds)
    }

    "find web auth endpoint" >> {
      roundTripDomainInfo("""WEB_AUTH_ENDPOINT="https://charlottes.web/"""")
        .map(_.webAuthEndpoint) must beSome(HttpUrl.parse("https://charlottes.web/"))
        .awaitFor(5.seconds)
    }

    "find public horizon url" >> {
      roundTripDomainInfo("""HORIZON_URL="https://event.horizon/"""")
        .map(_.horizonEndpoint) must beSome(HttpUrl.parse("https://event.horizon/"))
        .awaitFor(5.seconds)
    }

    "find signer key" >> {
      val kp = KeyPair.random
      roundTripDomainInfo(s"""SIGNER_KEY="${kp.accountId}"""")
        .map(_.signingKey) must beSome(kp.asPublicKey)
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
        .map(_.signingKey) must throwA[RestException]
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
        beSome(HttpUrl.parse("https://puppies.com/")).awaitFor(5.seconds)
    }

    "find logo" >> {
      roundTripDomainInfo(doc("ORG_LOGO", """"https://puppies.com/le_puppy.png"""")).map(
        _.issuerDocumentation.flatMap(_.logo)) must
        beSome(HttpUrl.parse("https://puppies.com/le_puppy.png")).awaitFor(5.seconds)
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
        beSome(HttpUrl.parse("https://puppies.com/address.pdf")).awaitFor(5.seconds)
    }

    "find phone number" >> {
      roundTripDomainInfo(doc("ORG_PHONE_NUMBER", """"1800-woof-woof"""")).map(_.issuerDocumentation
        .flatMap(_.phoneNumber)) must beSome("1800-woof-woof").awaitFor(5.seconds)
    }

    "find phone number attestation" >> {
      roundTripDomainInfo(doc("ORG_PHONE_NUMBER_ATTESTATION", """"https://puppies.com/phone.pdf"""")).map(
        _.issuerDocumentation.flatMap(_.phoneNumberAttestation)) must
        beSome(HttpUrl.parse("https://puppies.com/phone.pdf")).awaitFor(5.seconds)
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

  "principals parsing" should {
    implicit val arb: Arbitrary[PointOfContact] = Arbitrary(genPointOfContact)

    def doc(poc: PointOfContact): String =
      s"""[[PRINCIPALS]]
         |${poc.name.map(v => s"""name="$v"""").getOrElse("")}
         |${poc.email.map(v => s"""email="$v"""").getOrElse("")}
         |${poc.keybase.map(v => s"""keybase="$v"""").getOrElse("")}
         |${poc.telegram.map(v => s"""telegram="$v"""").getOrElse("")}
         |${poc.twitter.map(v => s"""twitter="$v"""").getOrElse("")}
         |${poc.github.map(v => s"""github="$v"""").getOrElse("")}
         |${poc.idPhotoHash.map(v => s"""id_photo_hash="$v"""").getOrElse("")}
         |${poc.verificationPhotoHash.map(v => s"""verification_photo_hash="$v"""").getOrElse("")}
       """.stripMargin

    "find the point of contact" >> prop { pocs: List[PointOfContact] =>
      val toml = pocs.map(doc).mkString("\n")
      DomainInfo.from(toml).pointsOfContact mustEqual pocs
    }
  }

  "currencies parsing" should {
    implicit val arb: Arbitrary[Currency] = Arbitrary(genCurrency)

    def doc(ccy: Currency): String = {
      val collateral = ccy.collateral match {
        case Nil => ""
        case xs =>
          s"""collateral_addresses=${xs.map(_.address).mkString("[\"", "\",\"", "\"]")}
             |collateral_address_messages=${xs.map(_.message).mkString("[\"", "\",\"", "\"]")}
             |collateral_address_signatures=${xs.map(_.proof).mkString("[\"", "\",\"", "\"]")}""".stripMargin
      }

      s"""[[CURRENCIES]]
         |${ccy.asset.map(_.code).map(v => s"""code${if (v.contains('?')) "_template" else ""}="$v"""").getOrElse("")}
         |${ccy.asset.map(_.issuer.accountId).map(v => s"""issuer="$v"""").getOrElse("")}
         |${ccy.status.map(v => s"""status="${v.toString.toLowerCase}"""").getOrElse("")}
         |display_decimals=${ccy.displayDecimals}
         |${ccy.name.map(v => s"""name="$v"""").getOrElse("")}
         |${ccy.description.map(v => s"""desc="$v"""").getOrElse("")}
         |${ccy.conditions.map(v => s"""conditions="$v"""").getOrElse("")}
         |${ccy.image.map(v => s"""image="$v"""").getOrElse("")}
         |${ccy.fixedQuantity.map(v => s"""fixed_number=$v""").getOrElse("")}
         |${ccy.maxQuantity.map(v => s"""max_number=$v""").getOrElse("")}
         |${ccy.isUnlimited.map(v => s"""is_unlimited=$v""").getOrElse("")}
         |${ccy.isAnchored.map(v => s"""is_asset_anchored=$v""").getOrElse("")}
         |${ccy.anchoredAssetType.map(v => s"""anchor_asset_type="$v"""").getOrElse("")}
         |${ccy.anchoredAsset.map(v => s"""anchor_asset="$v"""").getOrElse("")}
         |${ccy.redemptionInstructions.map(v => s"""redemption_instructions="$v"""").getOrElse("")}
         |$collateral
         |regulated=${ccy.isRegulated.toString}
         |${ccy.approvalServer.map(v => s"""approval_server="$v"""").getOrElse("")}
         |${ccy.approvalCriteria.map(v => s"""approval_criteria="$v"""").getOrElse("")}
         |""".stripMargin
    }

    "find the currency" >> prop { ccys: List[Currency] =>
      val toml = ccys.map(doc).mkString("\n")
      DomainInfo.from(toml).currencies mustEqual ccys
    }.setParameters(Parameters(minTestsOk = 5))
  }

  "validators parsing" should {
    implicit val arb: Arbitrary[Validator] = Arbitrary(genValidator)

    def doc(validator: Validator): String =
      s"""[[VALIDATORS]]
         |${validator.alias.map(v => s"""ALIAS="$v"""").getOrElse("")}
         |${validator.displayName.map(v => s"""DISPLAY_NAME="$v"""").getOrElse("")}
         |${validator.publicKey.map(v => s"""PUBLIC_KEY="${v.accountId}"""").getOrElse("")}
         |${validator.host.map(v => s"""HOST="$v"""").getOrElse("")}
         |${validator.history.map(v => s"""HISTORY="$v"""").getOrElse("")}
         |""".stripMargin

    "find the validator" >> prop { validators: List[Validator] =>
      val toml = validators.map(doc).mkString("\n")
      DomainInfo.from(toml).validators mustEqual validators
    }
  }

  private def roundTripDomainInfo(content: String): Future[DomainInfo] = {
    val server = new MockWebServer
    server.enqueue(new MockResponse().setBody(content))
    server.start()
    val eventualInfo = DomainInfo.forDomain(server.url("").toString).map(_.get)
    eventualInfo.onComplete(_ => server.shutdown())
    eventualInfo
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

package stellar.sdk.model.domain

import akka.http.scaladsl.model.Uri
import toml.Value
import toml.Value.{Str, Tbl}

/**
  * The Issuer Documentation subsection of the Domain Info parsed from stellar.toml files.
  *
  * @see https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0001.md#issuer-documentation
  */
case class IssuerDocumentation(name: Option[String] = None,
                               doingBusinessAs: Option[String] = None,
                               url: Option[Uri] = None,
                               logo: Option[Uri] = None,
                               description: Option[String] = None,
                               physicalAddress: Option[String] = None,
                               physicalAddressAttestation: Option[Uri] = None,
                               phoneNumber: Option[String] = None,
                               phoneNumberAttestation: Option[Uri] = None,
                               keybase: Option[String] = None,
                               twitter: Option[String] = None,
                               github: Option[String] = None,
                               email: Option[String] = None,
                               licensingAuthority: Option[String] = None,
                               licenseType: Option[String] = None,
                               licenseNumber: Option[String] = None,
                              )

object IssuerDocumentation {

  def parse(tbl: Tbl): IssuerDocumentation = {
    def parseTomlValue[T](key: String, parser: PartialFunction[Value, T]) =
      tbl.values.get(key).map(parser.applyOrElse(_, {
        v: Value => throw DomainInfoParseException(s"value for $key was not of the expected type. [value=$v]")
      }))

    IssuerDocumentation(
      name = parseTomlValue("ORG_NAME", { case Str(s) => s }),
      doingBusinessAs = parseTomlValue("ORG_DBA", { case Str(s) => s }),
      url = parseTomlValue("ORG_URL", { case Str(s) => Uri(s) }),
      logo = parseTomlValue("ORG_LOGO", { case Str(s) => Uri(s) }),
      description = parseTomlValue("ORG_DESCRIPTION", { case Str(s) => s }),
      physicalAddress = parseTomlValue("ORG_PHYSICAL_ADDRESS", { case Str(s) => s }),
      physicalAddressAttestation = parseTomlValue("ORG_PHYSICAL_ADDRESS_ATTESTATION", { case Str(s) => Uri(s) }),
      phoneNumber = parseTomlValue("ORG_PHONE_NUMBER", { case Str(s) => s }),
      phoneNumberAttestation = parseTomlValue("ORG_PHONE_NUMBER_ATTESTATION", { case Str(s) => Uri(s) }),
      keybase = parseTomlValue("ORG_KEYBASE", { case Str(s) => s }),
      twitter = parseTomlValue("ORG_TWITTER", { case Str(s) => s }),
      github = parseTomlValue("ORG_GITHUB", { case Str(s) => s }),
      email = parseTomlValue("ORG_OFFICIAL_EMAIL", { case Str(s) => s }),
      licensingAuthority = parseTomlValue("ORG_LICENSING_AUTHORITY", { case Str(s) => s }),
      licenseType = parseTomlValue("ORG_LICENSE_TYPE", { case Str(s) => s }),
      licenseNumber = parseTomlValue("ORG_LICENSE_NUMBER", { case Str(s) => s }),
    )
  }
}
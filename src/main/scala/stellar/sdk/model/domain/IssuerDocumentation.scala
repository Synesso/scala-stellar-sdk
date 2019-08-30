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

object IssuerDocumentation extends TomlParsers {

  def parse(tbl: Tbl): IssuerDocumentation = {
    def parseTomlValue[T](key: String, parser: PartialFunction[Value, T]) =
      super.parseTomlValue(tbl, key, parser)

    IssuerDocumentation(
      name = parseTomlValue("ORG_NAME", string),
      doingBusinessAs = parseTomlValue("ORG_DBA", string),
      url = parseTomlValue("ORG_URL", uri),
      logo = parseTomlValue("ORG_LOGO", uri),
      description = parseTomlValue("ORG_DESCRIPTION", string),
      physicalAddress = parseTomlValue("ORG_PHYSICAL_ADDRESS", string),
      physicalAddressAttestation = parseTomlValue("ORG_PHYSICAL_ADDRESS_ATTESTATION", uri),
      phoneNumber = parseTomlValue("ORG_PHONE_NUMBER", string),
      phoneNumberAttestation = parseTomlValue("ORG_PHONE_NUMBER_ATTESTATION", uri),
      keybase = parseTomlValue("ORG_KEYBASE", string),
      twitter = parseTomlValue("ORG_TWITTER", string),
      github = parseTomlValue("ORG_GITHUB", string),
      email = parseTomlValue("ORG_OFFICIAL_EMAIL", string),
      licensingAuthority = parseTomlValue("ORG_LICENSING_AUTHORITY", string),
      licenseType = parseTomlValue("ORG_LICENSE_TYPE", string),
      licenseNumber = parseTomlValue("ORG_LICENSE_NUMBER", string),
    )
  }
}
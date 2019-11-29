package stellar.sdk.model.domain

import okhttp3.HttpUrl
import toml.Value
import toml.Value.Tbl

/**
  * The Issuer Documentation subsection of the Domain Info parsed from stellar.toml files.
  *
  * @see https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0001.md#issuer-documentation
  */
case class IssuerDocumentation(name: Option[String] = None,
                               doingBusinessAs: Option[String] = None,
                               url: Option[HttpUrl] = None,
                               logo: Option[HttpUrl] = None,
                               description: Option[String] = None,
                               physicalAddress: Option[String] = None,
                               physicalAddressAttestation: Option[HttpUrl] = None,
                               phoneNumber: Option[String] = None,
                               phoneNumberAttestation: Option[HttpUrl] = None,
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
      url = parseTomlValue("ORG_URL", url),
      logo = parseTomlValue("ORG_LOGO", url),
      description = parseTomlValue("ORG_DESCRIPTION", string),
      physicalAddress = parseTomlValue("ORG_PHYSICAL_ADDRESS", string),
      physicalAddressAttestation = parseTomlValue("ORG_PHYSICAL_ADDRESS_ATTESTATION", url),
      phoneNumber = parseTomlValue("ORG_PHONE_NUMBER", string),
      phoneNumberAttestation = parseTomlValue("ORG_PHONE_NUMBER_ATTESTATION", url),
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
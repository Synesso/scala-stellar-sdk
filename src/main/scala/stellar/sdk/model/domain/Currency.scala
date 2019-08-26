package stellar.sdk.model.domain

import akka.http.scaladsl.model.Uri
import stellar.sdk.model.{Asset, NonNativeAsset}
import stellar.sdk.model.domain.Currency.{Collateral, Status, string}
import toml.Value
import toml.Value.Tbl

/**
  *
  * @param asset           The issued token. The asset code may be a template. `?` characters represent single character
  *                        wildcards. This allows the currency to represent multiple assets that share the same info.
  *                        An example is futures, where the only difference between issues is the date of the contract.
  *                        E.g. `CORN????????` to match codes such as `CORN20180604`. The absence of `?` characters
  *                        means the asset code is literal.
  * @param name            A short (<= 20 char) name for the currency
  * @param description     A human-readable description for the currency and what it represents.
  * @param status          Mark whether token is dead/for testing/for private use or is live and should be listed in
  *                        live exchanges.
  * @param displayDecimals Preference for number of decimals to show when a client displays currency balance. 0-7
  * @param conditions    Human-readable conditions for use of the token.
  * @param image         URL to a PNG image on a transparent background representing token.
  * @param fixedQuantity Fixed number of tokens, if the number of tokens issued will never change.
  * @param maxQuantity   Max number of tokens, if there is an upper limit to the number of tokens that will exist.
  * @param isUnlimited   Declares whether the number of tokens is dilutable at the issuer's discretion.
  * @param isAnchored    Declares whether the token is redeemable for an underlying asset.
  * @param anchoredAssetType     Type of asset anchored. For example, fiat, crypto, stock, bond, commodity, real-estate...
  * @param anchoredAsset If anchored token, code / symbol for asset that token is anchored to. E.g. USD, BTC, SBUX,
  *                      Address of real-estate investment property.
  * @param redemptionInstructions If anchored token, these are instructions to redeem the underlying asset from tokens.
  * @param collateral If this is an anchored crypto token, list of one or more collateral descriptors.
  * @param approvalServer URL of a sep0008 compliant approval service that signs validated transactions.
  * @param approvalCriteria a human readable string that explains the issuer's requirements for approving transactions.
  */
case class Currency(asset: Option[NonNativeAsset] = None,
                    name: Option[String] = None,
                    description: Option[String] = None,
                    status: Option[Status] = None,
                    displayDecimals: Int = 7,
                    conditions: Option[String] = None,
                    image: Option[Uri] = None,
                    fixedQuantity: Option[Int] = None,
                    maxQuantity: Option[Int] = None,
                    isUnlimited: Option[Boolean] = None,
                    isAnchored: Option[Boolean] = None,
                    anchoredAssetType: Option[String] = None,
                    anchoredAsset: Option[String] = None,
                    redemptionInstructions: Option[String] = None,
                    collateral: List[Collateral] = Nil,
                    isRegulated: Boolean = false,
                    approvalServer: Option[Uri] = None,
                    approvalCriteria: Option[String] = None,
                   )

object Currency extends TomlParsers {

  sealed trait Status
  case object Live extends Status
  case object Dead extends Status
  case object Test extends Status
  case object Private extends Status

  object Status {
    def parse(s: String): Status = s.toLowerCase match {
      case "live" => Live
      case "dead" => Dead
      case "test" => Test
      case "private" => Private
      case _ => throw DomainInfoParseException(s"Unexpected currency status [status=$s]")
    }
  }
  /**
    * @param address The public address that holds the anchored asset.
    * @param message A specially formatted message asserting the terms of the collateralisation.
    * @param proof   The given message signed with the private key of the given address.
    */
  case class Collateral(address: String, message: String, proof: String)

  def parse(tbl: Tbl): Currency = {
    def parseTomlValue[T](key: String, parser: PartialFunction[Value, T]) =
      tbl.values.get(key).map(parser.applyOrElse(_, {
        v: Value => throw DomainInfoParseException(s"value for $key was not of the expected type. [value=$v]")
      }))

    val asset = for {
      code <- parseTomlValue("code_template", string) orElse parseTomlValue("code", string)
      issuer <- parseTomlValue("issuer", publicKey)
    } yield Asset(code, issuer)

    val collateral = for {
      addresses <- parseTomlValue("collateral_addresses", array(string))
      messages <- parseTomlValue("collateral_address_messages", array(string))
      signatures <- parseTomlValue("collateral_address_signatures", array(string))
    } yield addresses zip messages zip signatures map {
      case ((address, message), signature) => Collateral(address, message, signature)
    }

    Currency(
      asset = asset,
      name = parseTomlValue("name", string),
      description = parseTomlValue("desc", string),
      status = parseTomlValue("status", string).map(Status.parse),
      displayDecimals = parseTomlValue("display_decimals", int).getOrElse(7),
      conditions = parseTomlValue("conditions", string),
      image = parseTomlValue("image", uri),
      fixedQuantity = parseTomlValue("fixed_number", int),
      maxQuantity = parseTomlValue("max_number", int),
      isUnlimited = parseTomlValue("is_unlimited", bool),
      isAnchored = parseTomlValue("is_asset_anchored", bool),
      anchoredAssetType = parseTomlValue("anchor_asset_type", string),
      anchoredAsset = parseTomlValue("anchor_asset", string),
      redemptionInstructions = parseTomlValue("redemption_instructions", string),
      collateral = collateral.getOrElse(List.empty),
      isRegulated = parseTomlValue("regulated", bool).getOrElse(false),
      approvalServer = parseTomlValue("approval_server", uri),
      approvalCriteria = parseTomlValue("approval_criteria", string),
    )
  }
}

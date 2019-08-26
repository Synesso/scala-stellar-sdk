package stellar.sdk.model.domain

import org.scalacheck.Gen
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.{IssuedAsset12, IssuedAsset4, NonNativeAsset}
import stellar.sdk.model.domain.Currency._

trait DomainInfoGenerators extends ArbitraryInput {

  val genPointOfContact: Gen[PointOfContact] = for {
    name <- Gen.option(Gen.identifier)
    email <- Gen.option(Gen.identifier)
    keybase <- Gen.option(Gen.identifier)
    telegram <- Gen.option(Gen.identifier)
    twitter <- Gen.option(Gen.identifier)
    github <- Gen.option(Gen.identifier)
    id_photo_hash <- Gen.option(genHash)
    verification_photo_hash <- Gen.option(genHash)
  } yield PointOfContact(
    name, email, keybase, telegram, twitter, github, id_photo_hash, verification_photo_hash)

  val genCollateral: Gen[Collateral] = for {
    address <- Gen.identifier
    message <- Gen.identifier
    proof <- genHash
  } yield Collateral(address, message, proof)

  val genAssetTemplate: Gen[NonNativeAsset] = {
    def replaceChars(s: String): String = {
      val chars = Gen.nonEmptyListOf(Gen.alphaNumChar).sample.get.toSet
      s.map { c => if (chars.contains(c)) '?' else c }
    }

    genNonNativeAsset.map {
      case IssuedAsset4(code, issuer) => IssuedAsset4(replaceChars(code), issuer)
      case IssuedAsset12(code, issuer) => IssuedAsset12(replaceChars(code), issuer)
    }
  }

  val genCurrency: Gen[Currency] = for {
    asset <- Gen.option(Gen.oneOf(genNonNativeAsset, genAssetTemplate))
    name <- Gen.option(Gen.identifier)
    description <- Gen.option(Gen.identifier)
    status <- Gen.option(Gen.oneOf(Live, Dead, Test, Private))
    displayDecimals <- Gen.choose(0, 7)
    conditions <- Gen.option(Gen.identifier)
    image <- Gen.option(genUri)
    fixedQuantity <- Gen.option(Gen.posNum[Int])
    maxQuantity <- Gen.option(Gen.posNum[Int])
    isUnlimited <- Gen.option(Gen.oneOf(false, true))
    isAnchored <- Gen.option(Gen.oneOf(false, true))
    assetType <- Gen.option(Gen.alphaNumStr)
    anchoredAsset <- Gen.option(Gen.alphaNumStr)
    redemptionInstructions <- Gen.option(Gen.alphaNumStr)
    collateral <- Gen.listOf(genCollateral)
    isRegulated <- Gen.oneOf(false, true)
    approvalServer <- Gen.option(genUri)
    approvalCriteria <- Gen.option(Gen.alphaNumStr)
  } yield Currency(
    asset, name, description, status, displayDecimals, conditions, image, fixedQuantity, maxQuantity,
    isUnlimited, isAnchored, assetType, anchoredAsset, redemptionInstructions, collateral,
    isRegulated, approvalServer, approvalCriteria)

}

package stellar.scala.sdk

import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.stellar.sdk.xdr.SignerKey
import stellar.scala.sdk.op._

import scala.util.Random

trait ArbitraryInput extends ScalaCheck {

  implicit def arbKeyPair: Arbitrary[KeyPair] = Arbitrary(genKeyPair)

  implicit def arbVerifyingKey: Arbitrary[VerifyingKey] = Arbitrary(genVerifyingKey)

  implicit def arbAccount: Arbitrary[Account] = Arbitrary(genAccount)

  implicit def arbAmount: Arbitrary[Amount] = Arbitrary(genAmount)

  implicit def arbNativeAmount: Arbitrary[NativeAmount] = Arbitrary(genNativeAmount)

  implicit def arbAsset: Arbitrary[Asset] = Arbitrary(genAsset)

  implicit def arbNonNativeAsset: Arbitrary[NonNativeAsset] = Arbitrary(genNonNativeAsset)

  implicit def arbSetOptionsOperation: Arbitrary[SetOptionsOperation] = Arbitrary(genSetOptionsOperation)

  implicit def arbPrice: Arbitrary[Price] = Arbitrary(genPrice)

  implicit def arbOperation: Arbitrary[Operation] = Arbitrary(genOperation)

  def genKeyPair: Gen[KeyPair] = Gen.oneOf(Seq(KeyPair.random))

  def genSignerKey: Gen[SignerKey] = genKeyPair.map(_.getXDRSignerKey)

  def genVerifyingKey: Gen[VerifyingKey] = genKeyPair.map(kp => VerifyingKey(kp.pk))

  def genAccount: Gen[Account] = for {
    kp <- genKeyPair
    seq <- Gen.posNum[Long]
  } yield {
    Account(kp, seq)
  }

  def genAmount: Gen[Amount] = for {
    units <- Gen.posNum[Long]
    asset <- genAsset
  } yield {
    Amount(units, asset)
  }

  def genNativeAmount: Gen[NativeAmount] = Gen.posNum[Long].map(NativeAmount.apply)

  def genCode(min: Int, max: Int): Gen[String] = Gen.choose(min, max).map(i => Random.alphanumeric.take(i).mkString)

  def genAsset: Gen[Asset] = Gen.oneOf(genAssetNative, genAsset4, genAsset12)

  def genAssetNative: Gen[Asset] = Gen.oneOf(Seq(AssetTypeNative))

  def genNonNativeAsset: Gen[NonNativeAsset] = Gen.oneOf(genAsset4, genAsset12)

  def genAsset4: Gen[AssetTypeCreditAlphaNum4] = for {
    code <- genCode(1, 4)
    keyPair <- genKeyPair
  } yield {
    AssetTypeCreditAlphaNum4(code, keyPair)
  }

  def genAsset12: Gen[AssetTypeCreditAlphaNum12] = for {
    code <- genCode(5, 12)
    keyPair <- genKeyPair
  } yield {
    AssetTypeCreditAlphaNum12(code, keyPair)
  }

  def genAssetPath: Gen[Seq[Asset]] = (for {
    qty <- Gen.choose(0, 5)
    path <- Gen.listOfN(qty, genAsset)
  } yield {
    path
  }).suchThat(as => as.distinct.lengthCompare(as.size) == 0)

  def genIssuerFlag: Gen[IssuerFlag] =
    Gen.oneOf(AuthorizationRequiredFlag, AuthorizationRevocableFlag, AuthorizationImmutableFlag)

  def genAccountMergeOperation: Gen[AccountMergeOperation] = for {
    destination <- genVerifyingKey
    source <- Gen.option(genKeyPair)
  } yield AccountMergeOperation(destination, source)

  def genAllowTrustOperation = for {
    trustor <- genVerifyingKey
    assetCode <- Gen.identifier.map(_.take(12))
    authorise <- Gen.oneOf(true, false)
    source <- Gen.option(genKeyPair)
  } yield AllowTrustOperation(trustor, assetCode, authorise, source)

  def genChangeTrustOperation = for {
    limit <- genAmount
    source <- Gen.option(genKeyPair)
  } yield ChangeTrustOperation(limit, source)

  def genCreateAccountOperation = for {
    destination <- genVerifyingKey
    startingBalance <- genNativeAmount
    source <- Gen.option(genKeyPair)
  } yield CreateAccountOperation(destination, startingBalance, source)

  def genCreatePassiveOfferOperation = for {
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    source <- Gen.option(genKeyPair)
  } yield CreatePassiveOfferOperation(selling, buying, price, source)

  def genInflationOperation = Gen.oneOf(Seq(InflationOperation))

  def genDeleteDataOperation = for {
    name <- Gen.identifier
    source <- Gen.option(genKeyPair)
  } yield DeleteDataOperation(name, source)

  def genWriteDataOperation = for {
    name <- Gen.identifier
    value <- Gen.nonEmptyListOf(Gen.posNum[Byte]).map(_.toArray)
    source <- Gen.option(genKeyPair)
  } yield WriteDataOperation(name, value, source)

  def genManageDataOperation = Gen.oneOf(genDeleteDataOperation, genWriteDataOperation)

  def genCreateOfferOperation = for {
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    source <- Gen.option(genKeyPair)
  } yield CreateOfferOperation(selling, buying, price, source)

  def genDeleteOfferOperation = for {
    id <- Gen.posNum[Long]
    source <- Gen.option(genKeyPair)
  } yield DeleteOfferOperation(id, source)

  def genUpdateOfferOperation = for {
    id <- Gen.posNum[Long]
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    source <- Gen.option(genKeyPair)
  } yield UpdateOfferOperation(id, selling, buying, price, source)

  def genManageOfferOperation = Gen.oneOf(genCreateOfferOperation, genDeleteOfferOperation, genUpdateOfferOperation)

  def genPathPaymentOperation = for {
    sendMax <- genAmount
    destAccount <- genVerifyingKey
    destAmount <- genAmount
    path <- Gen.listOf(genAsset)
    source <- Gen.option(genKeyPair)
  } yield PathPaymentOperation(sendMax, destAccount, destAmount, path, source)

  def genPaymentOperation = for {
    destAccount <- genVerifyingKey
    amount <- genAmount
    source <- Gen.option(genKeyPair)
  } yield PaymentOperation(destAccount, amount, source)

  def genSetOptionsOperation: Gen[SetOptionsOperation] = for {
      inflationDestination <- Gen.option(genVerifyingKey)
      clearFlags <- Gen.option(Gen.nonEmptyContainerOf[Set, IssuerFlag](genIssuerFlag))
      setFlags <- Gen.option(Gen.nonEmptyContainerOf[Set, IssuerFlag](genIssuerFlag))
      masterKeyWeight <- Gen.option(Gen.choose(0, 255))
      lowThreshold <- Gen.option(Gen.choose(0, 255))
      medThreshold <- Gen.option(Gen.choose(0, 255))
      highThreshold <- Gen.option(Gen.choose(0, 255))
      homeDomain <- Gen.option(Gen.identifier)
      signer <- Gen.option{ for {
        signer <- genSignerKey
        weight <- Gen.choose(0, 255)
      } yield (signer, weight)}
      sourceAccount <- Gen.option(genKeyPair)
    } yield op.SetOptionsOperation(inflationDestination, clearFlags, setFlags, masterKeyWeight, lowThreshold, medThreshold,
      highThreshold, homeDomain, signer, sourceAccount)

  def genOperation: Gen[Operation] = {
    Gen.oneOf(genAccountMergeOperation, genAllowTrustOperation, genChangeTrustOperation, genCreateAccountOperation,
      genCreatePassiveOfferOperation, genInflationOperation, genManageDataOperation, genManageOfferOperation,
      genPathPaymentOperation, genPaymentOperation, genSetOptionsOperation)
  }

  def genPrice: Gen[Price] = for {
    n <- Gen.posNum[Int]
    d <- Gen.posNum[Int]
  } yield Price(n, d)

}

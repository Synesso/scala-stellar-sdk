package stellar.sdk.model.ledger

import org.scalacheck.{Arbitrary, Gen}
import stellar.sdk.ArbitraryInput

trait LedgerEntryGenerators extends ArbitraryInput {

  // LedgerKeys
  val genAccountKey: Gen[AccountKey] = genKeyPair.map(AccountKey.apply)

  val genTrustLineKey: Gen[TrustLineKey] = for {
    account <- genKeyPair
    asset <- genNonNativeAsset
  } yield TrustLineKey(account, asset)

  val genOfferKey: Gen[OfferKey] = for {
    account <- genKeyPair
    offerId <- Gen.posNum[Long]
  } yield OfferKey(account, offerId)

  val genDataKey: Gen[DataKey] = for {
    account <- genKeyPair
    name <- Gen.identifier
  } yield DataKey(account, name)

  val genLedgerKey: Gen[LedgerKey] = Gen.oneOf(genAccountKey, genTrustLineKey, genOfferKey, genDataKey)

  implicit val arbLedgerKey = Arbitrary(genLedgerKey)

}

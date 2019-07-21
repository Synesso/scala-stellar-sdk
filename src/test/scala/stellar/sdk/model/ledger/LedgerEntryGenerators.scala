package stellar.sdk.model.ledger

import org.scalacheck.{Arbitrary, Gen}
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.op.IssuerFlag

trait LedgerEntryGenerators extends ArbitraryInput {

  // Misc
  val genLiabilities: Gen[Liabilities] = for {
    buying <- Gen.posNum[Long]
    selling <- Gen.posNum[Long]
  } yield Liabilities(buying, selling)

  // LedgerKeys
  val genAccountKey: Gen[AccountKey] = genPublicKey.map(AccountKey.apply)

  val genTrustLineKey: Gen[TrustLineKey] = for {
    account <- genPublicKey
    asset <- genNonNativeAsset
  } yield TrustLineKey(account, asset)

  val genOfferKey: Gen[OfferKey] = for {
    account <- genPublicKey
    offerId <- Gen.posNum[Long]
  } yield OfferKey(account, offerId)

  val genDataKey: Gen[DataKey] = for {
    account <- genPublicKey
    name <- Gen.identifier
  } yield DataKey(account, name)

  val genLedgerKey: Gen[LedgerKey] = Gen.oneOf(genAccountKey, genTrustLineKey, genOfferKey, genDataKey)

  implicit val arbLedgerKey = Arbitrary(genLedgerKey)


  // LedgerEntries
  val genAccountEntry: Gen[AccountEntry] = for {
    lastModifiedLedgerSeq <- Gen.posNum[Int]
    account <- genPublicKey
    balance <- Gen.posNum[Long]
    seqNum <- Gen.posNum[Long]
    numSubEntries <- Gen.posNum[Int]
    inflationDestination <- Gen.option(genPublicKey)
    flags <- Gen.containerOf[Set, IssuerFlag](genIssuerFlag)
    homeDomain <- Gen.option(Gen.identifier)
    thresholds <- genThresholds
    signers <- Gen.listOf(genSigner)
    liabilities <- Gen.option(genLiabilities)
  } yield AccountEntry(account, balance, seqNum, numSubEntries, inflationDestination, flags, homeDomain,
    thresholds, signers, liabilities, lastModifiedLedgerSeq)

  val genLedgerEntry: Gen[LedgerEntry] = Gen.oneOf(genAccountEntry, genAccountEntry)

  implicit val arbLedgerEntry = Arbitrary(genLedgerEntry)

}

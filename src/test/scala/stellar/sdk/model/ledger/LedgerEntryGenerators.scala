package stellar.sdk.model.ledger

import okio.ByteString
import org.scalacheck.{Arbitrary, Gen}
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.ClaimantGenerators.genClaimant
import stellar.sdk.model.{AccountId, ClaimableBalanceHashId, ClaimableBalanceIds, Claimant, ClaimantGenerators, LedgerThresholds}
import stellar.sdk.model.op.IssuerFlag

trait LedgerEntryGenerators extends ArbitraryInput {

  // Misc
  val genLiabilities: Gen[Liabilities] = for {
    buying <- Gen.posNum[Long]
    selling <- Gen.posNum[Long]
  } yield Liabilities(buying, selling)

  val genLedgerThresholds: Gen[LedgerThresholds] = for {
    baseThresholds <- genThresholds
    master <- Gen.choose(0, 255)
  } yield LedgerThresholds(master, baseThresholds.low, baseThresholds.med, baseThresholds.high)

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

  val genClaimableBalanceKey: Gen[ClaimableBalanceKey] = for {
    id <- ClaimableBalanceIds.genClaimableBalanceId
  } yield ClaimableBalanceKey(id)

  val genLedgerKey: Gen[LedgerKey] = Gen.oneOf(genAccountKey, genTrustLineKey, genOfferKey, genDataKey, genClaimableBalanceKey)

  implicit val arbLedgerKey: Arbitrary[LedgerKey] = Arbitrary(genLedgerKey)


  // LedgerEntries
  val genAccountEntry: Gen[AccountEntry] = for {
    account <- genPublicKey
    balance <- Gen.posNum[Long]
    seqNum <- Gen.posNum[Long]
    numSubEntries <- Gen.posNum[Int]
    inflationDestination <- Gen.option(genPublicKey)
    flags <- Gen.containerOf[Set, IssuerFlag](genIssuerFlag)
    homeDomain <- Gen.option(Gen.identifier)
    thresholds <- genLedgerThresholds
    signers <- Gen.listOf(genSigner)
    // ext v1
    liabilities <- Gen.option(genLiabilities)
    // ext v2
    numSponsored <- Gen.option(Gen.posNum[Int])
    numSponsoring <- Gen.posNum[Int]
    sponsorIds <- Gen.listOf(genPublicKey.map(_.toAccountId)).map(_.take(10))
  } yield AccountEntry(account, balance, seqNum, numSubEntries, inflationDestination, flags, homeDomain,
    thresholds, signers, liabilities,
    numSponsored = liabilities.flatMap(_ => numSponsored).getOrElse(0),
    numSponsoring = liabilities.flatMap(_ => numSponsored).map(_ => numSponsoring).getOrElse(0),
    signerSponsoringIds = liabilities.flatMap(_ => numSponsored).map(_ => sponsorIds).getOrElse(Nil)
  )

  val genTrustLineEntry: Gen[TrustLineEntry] = for {
    account <- genPublicKey
    asset <- genNonNativeAsset
    balance <- Gen.posNum[Long]
    limit <- Gen.posNum[Long]
    issuerAuthorized <- Gen.oneOf(true, false)
    liabilities <- Gen.option(genLiabilities)
  } yield TrustLineEntry(account, asset, balance, limit, issuerAuthorized, liabilities)

  val genDataEntry: Gen[DataEntry] = for {
    account <- genPublicKey
    name <- Gen.identifier
    value <- Gen.identifier.map(_.getBytes("UTF-8").toIndexedSeq)
  } yield DataEntry(account, name, value)

  val genClaimableBalanceEntry: Gen[ClaimableBalanceEntry] = for {
    id <- Gen.containerOfN[Array, Byte](32, Gen.posNum[Byte]).map(new ByteString(_)).map(ClaimableBalanceHashId)
    claimants <- Gen.chooseNum(1, 10).flatMap(Gen.containerOfN[List, Claimant](_, genClaimant))
    amount <- genAmount
  } yield ClaimableBalanceEntry(id, claimants, amount)

  val genLedgerEntryData: Gen[LedgerEntryData] = Gen.oneOf(
    genAccountEntry, genTrustLineEntry, genOfferEntry, genDataEntry, genClaimableBalanceEntry
  )

  val genLedgerEntry: Gen[LedgerEntry] = for {
    lastModifiedLedgerSeq <- Gen.posNum[Int]
    data <- genLedgerEntryData
    sponsorship <- Gen.option(genPublicKey.map(_.toAccountId))
  } yield LedgerEntry(lastModifiedLedgerSeq, data, sponsorship)

  implicit val arbLedgerEntry = Arbitrary(genLedgerEntry)


  // LedgerEntryChanges

  val genLedgerEntryCreate: Gen[LedgerEntryCreate] = genLedgerEntry.map(LedgerEntryCreate)
  val genLedgerEntryUpdate: Gen[LedgerEntryUpdate] = genLedgerEntry.map(LedgerEntryUpdate)
  val genLedgerEntryDelete: Gen[LedgerEntryDelete] = genLedgerKey.map(LedgerEntryDelete)
  val genLedgerEntryState: Gen[LedgerEntryState] = genLedgerEntry.map(LedgerEntryState)

  val genLedgerEntryChange: Gen[LedgerEntryChange] =
    Gen.oneOf(genLedgerEntryCreate, genLedgerEntryUpdate, genLedgerEntryDelete, genLedgerEntryState)

  implicit val arbLedgerEntryChange = Arbitrary(genLedgerEntryChange)


  // TransactionLedgerEntries
  val genTransactionLedgerEntriesV0: Gen[TransactionLedgerEntries] = for {
    entries <- genListOfNM(1, 10, genListOfNM(1, 10, genLedgerEntryChange))
  } yield TransactionLedgerEntries(Nil, entries, Nil)

  val genTransactionLedgerEntriesV1: Gen[TransactionLedgerEntries] = for {
    txnLevel <- genListOfNM(1, 10, genLedgerEntryChange)
    opLevel <- genListOfNM(1, 10, genListOfNM(1, 10, genLedgerEntryChange))
  } yield TransactionLedgerEntries(txnLevel, opLevel, Nil)

  val genTransactionLedgerEntriesV2: Gen[TransactionLedgerEntries] = for {
    txnLevelBefore <- genListOfNM(1, 10, genLedgerEntryChange)
    opLevel <- genListOfNM(1, 10, genListOfNM(1, 10, genLedgerEntryChange))
    txnLevelAfter <- genListOfNM(1, 10, genLedgerEntryChange)
  } yield TransactionLedgerEntries(txnLevelBefore, opLevel, txnLevelAfter)

  val genTransactionLedgerEntries: Gen[TransactionLedgerEntries] =
    Gen.oneOf(genTransactionLedgerEntriesV0, genTransactionLedgerEntriesV1, genTransactionLedgerEntriesV2)

  implicit val arbTransactionLedgerEntries = Arbitrary(genTransactionLedgerEntries)

}

package stellar.sdk.model.ledger

import org.scalacheck.{Arbitrary, Gen}
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.LedgerThresholds
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

  val genLedgerKey: Gen[LedgerKey] = Gen.oneOf(genAccountKey, genTrustLineKey, genOfferKey, genDataKey)

  implicit val arbLedgerKey = Arbitrary(genLedgerKey)


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
    liabilities <- Gen.option(genLiabilities)
  } yield AccountEntry(account, balance, seqNum, numSubEntries, inflationDestination, flags, homeDomain,
    thresholds, signers, liabilities)

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

  val genLedgerEntryData: Gen[(LedgerEntryData, Int)] = for {
    idx <- Gen.choose(0, 3)
    data <- List(genAccountEntry, genTrustLineEntry, genOfferEntry, genDataEntry)(idx)
  } yield data -> idx

  val genLedgerEntry: Gen[LedgerEntry] = for {
    lastModifiedLedgerSeq <- Gen.posNum[Int]
    (data, idx) <- genLedgerEntryData
  } yield LedgerEntry(lastModifiedLedgerSeq, data, idx)

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
  val genTransactionLedgerEntriesv0: Gen[TransactionLedgerEntries] = for {
    entries <- genListOfNM(1, 10, genListOfNM(1, 10, genLedgerEntryChange))
  } yield TransactionLedgerEntries(None, entries)

  val genTransactionLedgerEntriesv1: Gen[TransactionLedgerEntries] = for {
    txnLevel <- genListOfNM(1, 10, genLedgerEntryChange)
    opLevel <- genListOfNM(1, 10, genListOfNM(1, 10, genLedgerEntryChange))
  } yield TransactionLedgerEntries(Some(None, txnLevel), opLevel)

  val genTransactionLedgerEntriesv2: Gen[TransactionLedgerEntries] = for {
    txnLevelBefore <- genListOfNM(1, 10, genLedgerEntryChange)
    opLevel <- genListOfNM(1, 10, genListOfNM(1, 10, genLedgerEntryChange))
    txnLevelAfter <- genListOfNM(1, 10, genLedgerEntryChange)
  } yield TransactionLedgerEntries(Some(Some(txnLevelBefore), txnLevelAfter), opLevel)

  val genTransactionLedgerEntries: Gen[TransactionLedgerEntries] =
    Gen.oneOf(genTransactionLedgerEntriesv0, genTransactionLedgerEntriesv1, genTransactionLedgerEntriesv2)

  implicit val arbTransactionLedgerEntries = Arbitrary(genTransactionLedgerEntries)

}

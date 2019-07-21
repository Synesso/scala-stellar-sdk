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

  val genTrustLineEntry: Gen[TrustLineEntry] = for {
    lastModifiedLedgerSeq <- Gen.posNum[Int]
    account <- genPublicKey
    asset <- genNonNativeAsset
    balance <- Gen.posNum[Long]
    limit <- Gen.posNum[Long]
    issuerAuthorized <- Gen.oneOf(true, false)
    liabilities <- Gen.option(genLiabilities)
  } yield TrustLineEntry(account, asset, balance, limit, issuerAuthorized, liabilities, lastModifiedLedgerSeq)

  val genOfferEntry: Gen[OfferEntry] = for {
    lastModifiedLedgerSeq <- Gen.posNum[Int]
    account <- genPublicKey
    offerId <- Gen.posNum[Long]
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
  } yield OfferEntry(account, offerId, selling, buying, price, lastModifiedLedgerSeq)

  val genDataEntry: Gen[DataEntry] = for {
    lastModifiedLedgerSeq <- Gen.posNum[Int]
    account <- genPublicKey
    name <- Gen.identifier
    value <- Gen.identifier.map(_.getBytes("UTF-8"))
  } yield DataEntry(account, name, value, lastModifiedLedgerSeq)

  val genLedgerEntry: Gen[LedgerEntry] = Gen.oneOf(genAccountEntry, genTrustLineEntry, genOfferEntry, genDataEntry)

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
  } yield TransactionLedgerEntries(Some(txnLevel), opLevel)

  val genTransactionLedgerEntries: Gen[TransactionLedgerEntries] =
    Gen.oneOf(genTransactionLedgerEntriesv0, genTransactionLedgerEntriesv1)

  implicit val arbTransactionLedgerEntries = Arbitrary(genTransactionLedgerEntries)

}

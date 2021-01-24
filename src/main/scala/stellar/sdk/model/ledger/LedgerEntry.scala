package stellar.sdk.model.ledger

import org.stellar.xdr.AccountEntryExtensionV1.AccountEntryExtensionV1Ext
import org.stellar.xdr.AccountEntryExtensionV2.AccountEntryExtensionV2Ext
import org.stellar.xdr.ClaimableBalanceEntry.ClaimableBalanceEntryExt
import org.stellar.xdr.DataEntry.DataEntryExt
import org.stellar.xdr.LedgerEntryExtensionV1.LedgerEntryExtensionV1Ext
import org.stellar.xdr.OfferEntry.OfferEntryExt
import org.stellar.xdr.TrustLineEntry.TrustLineEntryExt
import org.stellar.xdr.{AccountEntryExtensionV1, AccountEntryExtensionV2, DataValue, Int64, LedgerEntryExtensionV1, LedgerEntryType, SequenceNumber, SponsorshipDescriptor, String32, String64, Uint32, XdrString, AccountEntry => XAccountEntry, ClaimableBalanceEntry => XClaimableBalanceEntry, DataEntry => XDataEntry, LedgerEntry => XLedgerEntry, OfferEntry => XOfferEntry, TrustLineEntry => XTrustLineEntry}
import stellar.sdk.PublicKeyOps
import stellar.sdk.model._
import stellar.sdk.model.op.{IssuerFlag, IssuerFlags}

case class LedgerEntry(
  lastModifiedLedgerSeq: Int,
  data: LedgerEntryData,
  sponsorship: Option[AccountId]
) {
  def xdr: XLedgerEntry = new XLedgerEntry.Builder()
    .data(data.xdr)
    .ext(new XLedgerEntry.LedgerEntryExt.Builder()
      .discriminant(if (sponsorship.isEmpty) 0 else 1)
      .v1(sponsorship.map(s => new LedgerEntryExtensionV1.Builder()
        .ext(new LedgerEntryExtensionV1Ext.Builder()
          .discriminant(0)
          .build())
        .sponsoringID(new SponsorshipDescriptor(s.accountIdXdr))
        .build()).orNull)
      .build())
    .lastModifiedLedgerSeq(new Uint32(lastModifiedLedgerSeq))
    .build()
}

object LedgerEntry {
  def decode(xdr: XLedgerEntry): LedgerEntry = LedgerEntry(
    lastModifiedLedgerSeq = xdr.getLastModifiedLedgerSeq.getUint32,
    data = LedgerEntryData.decode(xdr.getData),
    sponsorship = Option(xdr.getExt)
      .filter(_.getDiscriminant == 1)
      .map(_.getV1.getSponsoringID.getSponsorshipDescriptor)
      .map(AccountId.decode)
  )
}

sealed trait LedgerEntryData {
  def xdr: XLedgerEntry.LedgerEntryData
}

object LedgerEntryData {
  def decode(xdr: XLedgerEntry.LedgerEntryData): LedgerEntryData = xdr.getDiscriminant match {
    case LedgerEntryType.ACCOUNT => AccountEntry.decode(xdr.getAccount)
    case LedgerEntryType.CLAIMABLE_BALANCE => ClaimableBalanceEntry.decode(xdr.getClaimableBalance)
    case LedgerEntryType.DATA => DataEntry.decode(xdr.getData)
    case LedgerEntryType.OFFER => OfferEntry.decode(xdr.getOffer)
    case LedgerEntryType.TRUSTLINE => TrustLineEntry.decode(xdr.getTrustLine)
  }
}

/*
      AccountID accountID;      // master public key for this account
      int64 balance;            // in stroops
      SequenceNumber seqNum;    // last sequence number used for this account
      uint32 numSubEntries;     // number of sub-entries this account has
                                // drives the reserve
      AccountID* inflationDest; // Account to vote for during inflation
      uint32 flags;             // see AccountFlags

      string32 homeDomain; // can be used for reverse federation and memo lookup

      // fields used for signatures
      // thresholds stores unsigned bytes: [weight of master|low|medium|high]
      Thresholds thresholds;

      Signer signers<20>; // possible signers for this account

      // reserved for future use
      union switch (int v)
      {
      case 0:
          void;
      case 1:
          struct
          {
              Liabilities liabilities;

              union switch (int v)
              {
              case 0:
                  void;
              }
              ext;
          } v1;
      }
      ext;
  };
 */
case class AccountEntry(
  account: PublicKeyOps,
  balance: Long,
  seqNum: Long,
  numSubEntries: Int,
  inflationDestination: Option[PublicKeyOps],
  flags: Set[IssuerFlag],
  homeDomain: Option[String],
  thresholds: LedgerThresholds,
  signers: Seq[Signer],
  liabilities: Option[Liabilities],
  numSponsored: Int,
  numSponsoring: Int,
  signerSponsoringIds: List[AccountId]
) extends LedgerEntryData {
  override def xdr: XLedgerEntry.LedgerEntryData = {

    val extensionV1 = liabilities.map { lx =>
      val hasSponsorshipInfo = numSponsored != 0 || numSponsoring != 0 || signerSponsoringIds.nonEmpty
      val extensionV2: Option[AccountEntryExtensionV2] = if (hasSponsorshipInfo) Some(
        new AccountEntryExtensionV2.Builder()
          .ext(new AccountEntryExtensionV2Ext.Builder()
            .discriminant(0)
            .build())
          .numSponsored(new Uint32(numSponsored))
          .numSponsoring(new Uint32(numSponsoring))
          .signerSponsoringIDs(signerSponsoringIds.map(_.accountIdXdr).map(new SponsorshipDescriptor(_)).toArray)
        .build()
      ) else None

      new AccountEntryExtensionV1.Builder()
        .liabilities(lx.xdr)
        .ext(new AccountEntryExtensionV1Ext.Builder()
          .discriminant(if (extensionV2.isEmpty) 0 else 2)
          .v2(extensionV2.orNull)
          .build())
        .build()
    }

    new XLedgerEntry.LedgerEntryData.Builder()
      .discriminant(LedgerEntryType.ACCOUNT)
      .account(new XAccountEntry.Builder()
        .accountID(account.toAccountId.accountIdXdr)
        .balance(new Int64(balance))
        .ext(new XAccountEntry.AccountEntryExt.Builder()
          .discriminant(if (extensionV1.isEmpty) 0 else 1)
          .v1(extensionV1.orNull)
          .build())
        .flags(new Uint32(IssuerFlags.flagsToInt(flags)))
        .homeDomain(homeDomain.map(new XdrString(_)).map(new String32(_)).orNull)
        .inflationDest(inflationDestination.map(_.toAccountId.accountIdXdr).orNull)
        .numSubEntries(new Uint32(numSubEntries))
        .seqNum(new SequenceNumber(new Int64(seqNum)))
        .signers(signers.toArray.map(_.xdr))
        .thresholds(thresholds.xdr)
        .build())
      .build()
  }
}

object AccountEntry {
  def decode(xdr: XAccountEntry): AccountEntry = {
    // decode 2-level extension data
    val (liabilities, numSponsored, numSponsoring, signerSponsoringIds) = xdr.getExt.getDiscriminant.intValue() match {
      case 0 => (None, 0, 0, Nil)
      case 1 => {
        val lx = Some(Liabilities.decode(xdr.getExt.getV1.getLiabilities))
        val ext2 = xdr.getExt.getV1.getExt
        ext2.getDiscriminant.intValue() match {
          case 0 => (lx, 0, 0, Nil)
          case 2 => (
            lx,
            ext2.getV2.getNumSponsored.getUint32.toInt,
            ext2.getV2.getNumSponsoring.getUint32.toInt,
            ext2.getV2.getSignerSponsoringIDs.map(_.getSponsorshipDescriptor).map(AccountId.decode).toList
          )
        }
      }
    }

    AccountEntry(
      account = AccountId.decode(xdr.getAccountID).publicKey,
      balance = xdr.getBalance.getInt64,
      seqNum = xdr.getSeqNum.getSequenceNumber.getInt64,
      numSubEntries = xdr.getNumSubEntries.getUint32,
      inflationDestination = Option(xdr.getInflationDest).map(AccountId.decode).map(_.publicKey),
      flags = IssuerFlags.from(xdr.getFlags.getUint32),
      homeDomain = Option(xdr.getHomeDomain).map(_.getString32.toString),
      thresholds = LedgerThresholds.decode(xdr.getThresholds),
      signers = xdr.getSigners.map(Signer.decode),
      liabilities = liabilities,
      numSponsored = numSponsored,
      numSponsoring = numSponsoring,
      signerSponsoringIds = signerSponsoringIds
    )
  }
}

/*
  struct TrustLineEntry
  {
      AccountID accountID; // account this trustline belongs to
      Asset asset;         // type of asset (with issuer)
      int64 balance;       // how much of this asset the user has.
                           // Asset defines the unit for this;

      int64 limit;  // balance cannot be above this
      uint32 flags; // see TrustLineFlags

      // reserved for future use
      union switch (int v)
      {
      case 0:
          void;
      case 1:
          struct
          {
              Liabilities liabilities;

              union switch (int v)
              {
              case 0:
                  void;
              }
              ext;
          } v1;
      }
      ext;
  };
 */
case class TrustLineEntry(account: PublicKeyOps, asset: NonNativeAsset, balance: Long, limit: Long,
                          issuerAuthorized: Boolean, liabilities: Option[Liabilities])
  extends LedgerEntryData {

  override def xdr: XLedgerEntry.LedgerEntryData =
    new XLedgerEntry.LedgerEntryData.Builder()
      .discriminant(LedgerEntryType.TRUSTLINE)
      .trustLine(new XTrustLineEntry.Builder()
        .accountID(account.toAccountId.accountIdXdr)
        .asset(asset.xdr)
        .balance(new Int64(balance))
        .flags(new Uint32(if (issuerAuthorized) 1 else 0))
        .limit(new Int64(limit))
        .ext(new XTrustLineEntry.TrustLineEntryExt.Builder()
          .discriminant(if (liabilities.isEmpty) 0 else 1)
          .v1(liabilities.map { lx =>
            new TrustLineEntryExt.TrustLineEntryV1.Builder()
              .liabilities(lx.xdr)
              .ext(new TrustLineEntryExt.TrustLineEntryV1.TrustLineEntryV1Ext.Builder()
                .discriminant(0)
                .build())
              .build()
          }.orNull)
          .build())
        .build())
      .build()
}

object TrustLineEntry {
  def decode(xdr: XTrustLineEntry): TrustLineEntry = TrustLineEntry(
    account = AccountId.decode(xdr.getAccountID).publicKey,
    asset = Asset.decode(xdr.getAsset).asInstanceOf[NonNativeAsset],
    balance = xdr.getBalance.getInt64,
    limit = xdr.getLimit.getInt64,
    issuerAuthorized = xdr.getFlags.getUint32 == 1,
    liabilities = xdr.getExt.getDiscriminant.intValue() match {
      case 0 => None
      case 1 => Some(Liabilities.decode(xdr.getExt.getV1.getLiabilities))
    }
  )
}

/*
  struct OfferEntry
  {
      AccountID sellerID;
      int64 offerID;
      Asset selling; // A
      Asset buying;  // B
      int64 amount;  // amount of A

      /* price for this offer:
          price of A in terms of B
          price=AmountB/AmountA=priceNumerator/priceDenominator
          price is after fees
      */
      Price price;
      uint32 flags; // see OfferEntryFlags

      // reserved for future use
      union switch (int v)
      {
      case 0:
          void;
      }
      ext;
  };
 */
case class OfferEntry(account: PublicKeyOps, offerId: Long, selling: Amount, buying: Asset, price: Price)
  extends LedgerEntryData {
  override def xdr: XLedgerEntry.LedgerEntryData =
    new XLedgerEntry.LedgerEntryData.Builder()
      .discriminant(LedgerEntryType.OFFER)
      .offer(new XOfferEntry.Builder()
        .amount(new Int64(selling.units))
        .buying(buying.xdr)
        .flags(new Uint32(0))
        .offerID(new Int64(offerId))
        .price(price.xdr)
        .sellerID(account.toAccountId.accountIdXdr)
        .selling(selling.asset.xdr)
        .ext(new OfferEntryExt.Builder()
          .discriminant(0)
          .build())
        .build())
      .build()
}

object OfferEntry {
  def decode(xdr: XOfferEntry): OfferEntry = OfferEntry(
    account = AccountId.decode(xdr.getSellerID).publicKey,
    offerId = xdr.getOfferID.getInt64,
    selling = Amount(xdr.getAmount.getInt64, Asset.decode(xdr.getSelling)),
    buying = Asset.decode(xdr.getBuying),
    price = Price.decode(xdr.getPrice)
  )
}

/*
  struct DataEntry
  {
      AccountID accountID; // account this data belongs to
      string64 dataName;
      DataValue dataValue;

      // reserved for future use
      union switch (int v)
      {
      case 0:
          void;
      }
      ext;
  };
 */
case class DataEntry(account: PublicKeyOps, name: String, value: Seq[Byte])
  extends LedgerEntryData {
  override def xdr: XLedgerEntry.LedgerEntryData =
    new XLedgerEntry.LedgerEntryData.Builder()
      .discriminant(LedgerEntryType.DATA)
      .data(new XDataEntry.Builder()
        .accountID(account.toAccountId.accountIdXdr)
        .dataName(new String64(new XdrString(name)))
        .dataValue(new DataValue(value.toArray))
        .ext(new DataEntryExt.Builder()
          .discriminant(0)
          .build())
        .build())
      .build()
}

object DataEntry {
  def decode(xdr: XDataEntry): DataEntry = DataEntry(
    account = AccountId.decode(xdr.getAccountID).publicKey,
    name = xdr.getDataName.toString,
    value = xdr.getDataValue.getDataValue
  )
}

/*
struct ClaimableBalanceEntry
{
    // Unique identifier for this ClaimableBalanceEntry
    ClaimableBalanceID balanceID;

    // List of claimants with associated predicate
    Claimant claimants<10>;

    // Any asset including native
    Asset asset;

    // Amount of asset
    int64 amount;

    // reserved for future use
    union switch (int v)
    {
    case 0:
        void;
    }
    ext;
};
 */
case class ClaimableBalanceEntry(
  id: ClaimableBalanceId,
  claimants: List[Claimant],
  amount: Amount
) extends LedgerEntryData {
  override def xdr: XLedgerEntry.LedgerEntryData =
    new XLedgerEntry.LedgerEntryData.Builder()
      .discriminant(LedgerEntryType.CLAIMABLE_BALANCE)
      .claimableBalance(new XClaimableBalanceEntry.Builder()
        .amount(new Int64(amount.units))
        .asset(amount.asset.xdr)
        .balanceID(id.xdr)
        .claimants(claimants.map(_.xdr).toArray)
        .ext(new ClaimableBalanceEntryExt.Builder()
          .discriminant(0)
          .build())
        .build())
      .build()
}

object ClaimableBalanceEntry {
  def decode(xdr: XClaimableBalanceEntry): ClaimableBalanceEntry = ClaimableBalanceEntry(
    id = ClaimableBalanceId.decode(xdr.getBalanceID),
    claimants = xdr.getClaimants.map(Claimant.decode).toList,
    amount = Amount(xdr.getAmount.getInt64, Asset.decode(xdr.getAsset))
  )
}

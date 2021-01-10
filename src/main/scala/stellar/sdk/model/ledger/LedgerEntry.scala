package stellar.sdk.model.ledger

import okio.ByteString
import org.stellar.xdr.AccountEntryExtensionV1.AccountEntryExtensionV1Ext
import org.stellar.xdr.LedgerEntryExtensionV1.LedgerEntryExtensionV1Ext
import org.stellar.xdr.{AccountEntryExtensionV1, Int64, LedgerEntryExtensionV1, LedgerEntryType, SponsorshipDescriptor, Uint32, AccountEntry => XAccountEntry, LedgerEntry => XLedgerEntry}
import stellar.sdk.PublicKeyOps
import stellar.sdk.model._
import stellar.sdk.model.op.IssuerFlag

sealed trait LedgerEntryData {
  def xdr: XLedgerEntry.LedgerEntryData
}

case class LedgerEntry(
  lastModifiedLedgerSeq: Int,
  data: LedgerEntryData,
  sponsorship: Option[AccountId],
  private val dataDisc: Int
) {
  def xdr: XLedgerEntry = new XLedgerEntry.Builder()
    .data(data.xdr)
    .ext(new XLedgerEntry.LedgerEntryExt.Builder()
      .discriminant(sponsorship.map(_ => 1).getOrElse(0))
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
    // TODO - Congrats for making it this far!! Let's keep going!!
    val extension1 = liabilities.map { l =>
      val hasSponsorshipInfo = numSponsored != 0 || numSponsoring != 0 || signerSponsoringIds.nonEmpty
      val extension2 = if (hasSponsorshipInfo) Some(
        new AccountEntryExtensionV1Ext.Builder()
          .
          .build()
      ) else None
      new AccountEntryExtensionV1.Builder()
        .liabilities(l.xdr)
        .ext()
        .build()
    }



    val extension: Option[AccountEntryExtensionV1] = if (hasSponsorshipInfo) {
      Some(new AccountEntryExtensionV1.Builder()
        .
        .build())
    } else None
    new XLedgerEntry.LedgerEntryData.Builder()
      .discriminant(LedgerEntryType.ACCOUNT)
      .account(new XAccountEntry.Builder()
        .accountID(account.toAccountId.accountIdXdr)
        .balance(new Int64(balance))
        .ext(new XAccountEntry.AccountEntryExt.Builder()
          .discriminant(if (hasSponsorshipInfo) 1 else 0)
          .v1(if (hasSponsorshipInfo))
          .build())
        .flags()
        .homeDomain()
        .inflationDest()
        .numSubEntries()
        .seqNum()
        .signers()
        .thresholds()
        .build())
      .build()
  }
}

object AccountEntry {
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

}

object TrustLineEntry {
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
}

object OfferEntry {
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

}

object DataEntry {
}

/*
struct ClaimableBalanceEntry
{
    // Unique identifier for this ClaimableBalanceEntry
    ClaimableBalanceID balanceID;

    // Account that created this ClaimableBalanceEntry
    AccountID createdBy;

    // List of claimants with associated predicate
    Claimant claimants<10>;

    // Any asset including native
    Asset asset;

    // Amount of asset
    int64 amount;

    // Amount of native asset to pay the reserve
    int64 reserve;

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
  id: ByteString,
  createdBy: PublicKeyOps,
  claimants: List[Claimant],
  amount: Amount,
  reserve: NativeAmount
) extends LedgerEntryData {

}

object ClaimableBalanceEntry {
}

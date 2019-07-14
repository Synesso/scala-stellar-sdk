package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.{KeyPair, PublicKey}
import stellar.sdk.model.{Amount, Asset, Price, Signer, Thresholds}
import stellar.sdk.model.op.{IssuerFlag, IssuerFlags}
import stellar.sdk.model.xdr.Decode

sealed trait LedgerEntry

object LedgerEntry extends Decode {
  private def widen[A, W, O <: W](s: State[A, O]): State[A, W] = s.map(w => w: W)

  def decode: State[Seq[Byte], LedgerEntry] = switch(
    widen(AccountEntry.decode),
    widen(TrustLineEntry.decode),
    widen(OfferEntry.decode),
    widen(DataEntry.decode)
  )
}

/*
//      AccountID accountID;      // master public key for this account
//      int64 balance;            // in stroops
//      SequenceNumber seqNum;    // last sequence number used for this account
//      uint32 numSubEntries;     // number of sub-entries this account has
//                                // drives the reserve
//      AccountID* inflationDest; // Account to vote for during inflation
//      uint32 flags;             // see AccountFlags
//
//      string32 homeDomain; // can be used for reverse federation and memo lookup
//
//      // fields used for signatures
//      // thresholds stores unsigned bytes: [weight of master|low|medium|high]
//      Thresholds thresholds;
//
//      Signer signers<20>; // possible signers for this account
//
//      // reserved for future use
//      union switch (int v)
//      {
//      case 0:
//          void;
//      case 1:
//          struct
//          {
//              Liabilities liabilities;
//
//              union switch (int v)
//              {
//              case 0:
//                  void;
//              }
//              ext;
//          } v1;
//      }
//      ext;
//  };

 */
case class AccountEntry(account: PublicKey, balance: Long, seqNum: Long, numSubEntries: Int,
                        inflationDestination: Option[PublicKey], flags: Set[IssuerFlag],
                        homeDomain: Option[String], thresholds: Thresholds, signers: Seq[Signer],
                        liabilities: Option[Liabilities]) extends LedgerEntry

object AccountEntry extends Decode {
  val decode: State[Seq[Byte], AccountEntry] = for {
    account <- KeyPair.decode
    balance <- long
    seqNum <- long
    numSubEntries <- int
    inflationDestination <- opt(KeyPair.decode)
    flags <- IssuerFlags.decode
    homeDomain <- string.map(Some(_).filter(_.nonEmpty))
    thresholds <- Thresholds.decode
    signers <- arr(Signer.decode)
    liabilities <- switch[Option[Liabilities]](
      State.pure(None),
      Liabilities.decode.map(Some(_))
    )
  } yield AccountEntry(account, balance, seqNum, numSubEntries, inflationDestination, flags, homeDomain, thresholds,
      signers, liabilities)
}
/*
//  struct TrustLineEntry
//  {
//      AccountID accountID; // account this trustline belongs to
//      Asset asset;         // type of asset (with issuer)
//      int64 balance;       // how much of this asset the user has.
//                           // Asset defines the unit for this;
//
//      int64 limit;  // balance cannot be above this
//      uint32 flags; // see TrustLineFlags
//
//      // reserved for future use
//      union switch (int v)
//      {
//      case 0:
//          void;
//      case 1:
//          struct
//          {
//              Liabilities liabilities;
//
//              union switch (int v)
//              {
//              case 0:
//                  void;
//              }
//              ext;
//          } v1;
//      }
//      ext;
//  };

 */
case class TrustLineEntry(account: PublicKey, asset: Asset, balance: Long, limit: Long,
                          issuerAuthorized: Boolean, liabilities: Option[Liabilities]) extends LedgerEntry

object TrustLineEntry extends Decode {
  val decode: State[Seq[Byte], TrustLineEntry] = for {
    account <- KeyPair.decode
    asset <- Asset.decode
    balance <- long
    limit <- long
    issuerAuthorized <- bool
    liabilities <- switch[Option[Liabilities]](
      State.pure(None),
      Liabilities.decode.map(Some(_))
    )
  } yield TrustLineEntry(account, asset, balance, limit, issuerAuthorized, liabilities)
}

/*
//  struct OfferEntry
//  {
//      AccountID sellerID;
//      int64 offerID;
//      Asset selling; // A
//      Asset buying;  // B
//      int64 amount;  // amount of A
//
//      /* price for this offer:
//          price of A in terms of B
//          price=AmountB/AmountA=priceNumerator/priceDenominator
//          price is after fees
//      */
//      Price price;
//      uint32 flags; // see OfferEntryFlags
//
//      // reserved for future use
//      union switch (int v)
//      {
//      case 0:
//          void;
//      }
//      ext;
//  };
 */
case class OfferEntry(account: PublicKey, offerId: Long, selling: Amount, buying: Asset, price: Price) extends LedgerEntry

object OfferEntry extends Decode {
  val decode: State[Seq[Byte], OfferEntry] = for {
    account <- KeyPair.decode
    offerId <- long
    selling <- Asset.decode
    buying <- Asset.decode
    units <- long
    price <- Price.decode
    flags <- int
    _ = assert(flags == 1)
    _ <- int
  } yield OfferEntry(account, offerId, Amount(units, selling), buying, price)
}


/*
//  struct DataEntry
//  {
//      AccountID accountID; // account this data belongs to
//      string64 dataName;
//      DataValue dataValue;
//
//      // reserved for future use
//      union switch (int v)
//      {
//      case 0:
//          void;
//      }
//      ext;
//  };

 */
case class DataEntry(account: PublicKey, name: String, value: Seq[Byte]) extends LedgerEntry

object DataEntry extends Decode {
  val decode: State[Seq[Byte], DataEntry] = for {
    account <- KeyPair.decode
    name <- string
    value <- bytes
    _ <- int
  } yield DataEntry(account, name, value)
}

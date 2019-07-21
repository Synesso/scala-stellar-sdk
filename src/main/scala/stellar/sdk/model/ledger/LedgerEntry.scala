package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.op.{IssuerFlag, IssuerFlags}
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.model._
import stellar.sdk.{KeyPair, PublicKeyOps}

sealed trait LedgerEntry extends Encodable {
  val lastModifiedLedgerSeq: Int
}

object LedgerEntry extends Decode {
  val decode: State[Seq[Byte], LedgerEntry] = for {
    lastModifiedLedgerSeq <- int
    entry <- switch[LedgerEntry](
      widen(AccountEntry.decode.map(_.copy(lastModifiedLedgerSeq = lastModifiedLedgerSeq))),
      widen(TrustLineEntry.decode.map(_.copy(lastModifiedLedgerSeq = lastModifiedLedgerSeq))),
      widen(OfferEntry.decode.map(_.copy(lastModifiedLedgerSeq = lastModifiedLedgerSeq))),
      widen(DataEntry.decode.map(_.copy(lastModifiedLedgerSeq = lastModifiedLedgerSeq)))
    )
  } yield entry
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
case class AccountEntry(account: PublicKeyOps, balance: Long, seqNum: Long, numSubEntries: Int,
                        inflationDestination: Option[PublicKeyOps], flags: Set[IssuerFlag],
                        homeDomain: Option[String], thresholds: Thresholds, signers: Seq[Signer],
                        liabilities: Option[Liabilities], lastModifiedLedgerSeq: Int) extends LedgerEntry {

  override def encode: Stream[Byte] = Encode.int(lastModifiedLedgerSeq) ++
    Encode.int(0) ++
    account.encode ++
    Encode.long(balance) ++
    Encode.long(seqNum) ++
    Encode.int(numSubEntries) ++
    Encode.opt(inflationDestination) ++
    Encode.int(flags.map(_.i + 0).fold(0)(_ | _)) ++
    Encode.string(homeDomain.getOrElse("")) ++
    thresholds.encode ++
    Encode.arr(signers) ++
    Encode.opt(liabilities)

}

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
    liabilities <- opt(Liabilities.decode)
  } yield AccountEntry(account, balance, seqNum, numSubEntries, inflationDestination, flags, homeDomain, thresholds,
      signers, liabilities, -1)
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
case class TrustLineEntry(account: PublicKeyOps, asset: NonNativeAsset, balance: Long, limit: Long,
                          issuerAuthorized: Boolean, liabilities: Option[Liabilities], lastModifiedLedgerSeq: Int)
  extends LedgerEntry {

  override def encode: Stream[Byte] = Encode.int(lastModifiedLedgerSeq) ++
    Encode.int(1) ++
    account.encode ++
    asset.encode ++
    Encode.long(balance) ++
    Encode.long(limit) ++
    Encode.bool(issuerAuthorized) ++
    Encode.opt(liabilities)

}

object TrustLineEntry extends Decode {
  val decode: State[Seq[Byte], TrustLineEntry] = for {
    account <- KeyPair.decode
    asset <- Asset.decode.map(_.asInstanceOf[NonNativeAsset])
    balance <- long
    limit <- long
    issuerAuthorized <- bool
    liabilities <- switch[Option[Liabilities]](
      State.pure(None),
      Liabilities.decode.map(Some(_))
    )
  } yield TrustLineEntry(account, asset, balance, limit, issuerAuthorized, liabilities, -1)
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
case class OfferEntry(account: PublicKeyOps, offerId: Long, selling: Amount, buying: Asset, price: Price,
                      lastModifiedLedgerSeq: Int) extends LedgerEntry {
  override def encode: Stream[Byte] = ???
}

object OfferEntry extends Decode {
  val decode: State[Seq[Byte], OfferEntry] = for {
    account <- KeyPair.decode
    offerId <- long
    selling <- Asset.decode
    buying <- Asset.decode
    units <- long
    price <- Price.decode
    _ <- int // flags
    _ <- int // ext
  } yield OfferEntry(account, offerId, Amount(units, selling), buying, price, -1)
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
case class DataEntry(account: PublicKeyOps, name: String, value: Seq[Byte], lastModifiedLedgerSeq: Int) extends LedgerEntry {
  override def encode: Stream[Byte] = ???
}

object DataEntry extends Decode {
  val decode: State[Seq[Byte], DataEntry] = for {
    account <- KeyPair.decode
    name <- string
    value <- bytes
    _ <- int
  } yield DataEntry(account, name, value, -1)
}

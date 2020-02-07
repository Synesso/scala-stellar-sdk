package stellar.sdk.model.ledger

import cats.data.State
import stellar.sdk.model.op.{IssuerFlag, IssuerFlags}
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.model._
import stellar.sdk.{KeyPair, PublicKeyOps}

sealed trait LedgerEntryData extends Encodable

case class LedgerEntry(lastModifiedLedgerSeq: Int, data: LedgerEntryData, private val dataDisc: Int) extends Encodable {
  override def encode: LazyList[Byte] = Encode.int(lastModifiedLedgerSeq) ++ Encode.int(dataDisc) ++ data.encode
}

object LedgerEntry extends Decode {
  val decode: State[Seq[Byte], LedgerEntry] = for {
    lastModifiedLedgerSeq <- int
    dataDisc <- switchInt[LedgerEntryData](
      widen(AccountEntry.decode),
      widen(TrustLineEntry.decode),
      widen(OfferEntry.decode),
      widen(DataEntry.decode)
    )
    (data, disc) = dataDisc
  } yield LedgerEntry(lastModifiedLedgerSeq, data, disc)
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
case class AccountEntry(account: PublicKeyOps, balance: Long, seqNum: Long, numSubEntries: Int,
                        inflationDestination: Option[PublicKeyOps], flags: Set[IssuerFlag],
                        homeDomain: Option[String], thresholds: LedgerThresholds, signers: Seq[Signer],
                        liabilities: Option[Liabilities]) extends LedgerEntryData {

  override def encode: LazyList[Byte] =
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
    thresholds <- LedgerThresholds.decode
    signers <- arr(Signer.decode)
    liabilities <- opt(Liabilities.decode)
  } yield AccountEntry(account, balance, seqNum, numSubEntries, inflationDestination, flags, homeDomain, thresholds,
      signers, liabilities)
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

  override def encode: LazyList[Byte] =
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
  } yield TrustLineEntry(account, asset, balance, limit, issuerAuthorized, liabilities)
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

  override def encode: LazyList[Byte] =
    account.encode ++
    Encode.long(offerId) ++
    selling.asset.encode ++
    buying.encode ++
    Encode.long(selling.units) ++
    price.encode ++
    Encode.long(0)

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
  } yield OfferEntry(account, offerId, Amount(units, selling), buying, price)
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

  override def encode: LazyList[Byte] =
    account.encode ++
    Encode.string(name) ++
    Encode.padded(value) ++
    Encode.int(0)
}

object DataEntry extends Decode {
  val decode: State[Seq[Byte], DataEntry] = for {
    account <- KeyPair.decode
    name <- string
    value <- padded()
    _ <- int
  } yield DataEntry(account, name, value)
}

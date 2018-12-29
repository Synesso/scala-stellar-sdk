package stellar.sdk

import org.apache.commons.codec.binary.Hex
import org.specs2.matcher.{AnyMatchers, Matcher, MustExpectations, OptionMatchers, SequenceMatchersCreation}
import stellar.sdk.ByteArrays.base64
import stellar.sdk.op._
import stellar.sdk.res.TransactionHistory

trait DomainMatchers extends AnyMatchers with MustExpectations with SequenceMatchersCreation with OptionMatchers {

  def beEquivalentTo(other: Asset): Matcher[Asset] = beLike[Asset] {
    case NativeAsset =>
      other mustEqual NativeAsset
    case IssuedAsset4(code, issuer) =>
      val IssuedAsset4(expectedCode, expectedIssuer) = other
      code mustEqual expectedCode
      issuer.accountId mustEqual expectedIssuer.accountId
    case IssuedAsset12(code, issuer) =>
      val IssuedAsset12(expectedCode, expectedIssuer) = other
      code mustEqual expectedCode
      issuer.accountId mustEqual expectedIssuer.accountId
  }

  def beEquivalentTo(other: Amount): Matcher[Amount] = beLike[Amount] {
    case NativeAmount(units) =>
      units mustEqual other.units
      other.asset mustEqual NativeAsset
    case IssuedAmount(units, asset) =>
      units mustEqual other.units
      asset must beEquivalentTo(other.asset)
  }

  def beEquivalentTo(other: KeyPair): Matcher[KeyPair] = beLike[KeyPair] {
    case KeyPair(pk, sk) =>
      Hex.encodeHex(pk.getAbyte) mustEqual Hex.encodeHex(other.pk.getAbyte)
      Hex.encodeHex(sk.getAbyte) mustEqual Hex.encodeHex(other.sk.getAbyte)
  }

  def beEquivalentTo(other: PublicKeyOps): Matcher[PublicKeyOps] = beLike[PublicKeyOps] {
    case pk =>
      pk.accountId mustEqual other.accountId
  }

  def beEquivalentTo(other: AccountMergeOperation): Matcher[AccountMergeOperation] = beLike[AccountMergeOperation] {
    case op =>
      op.destination.accountId mustEqual other.destination.accountId
  }

  def beEquivalentTo(other: AllowTrustOperation): Matcher[AllowTrustOperation] = beLike[AllowTrustOperation] {
    case op =>
      op.trustor.accountId mustEqual other.trustor.accountId
      op.assetCode mustEqual other.assetCode
      op.authorize mustEqual other.authorize
  }

  def beEquivalentTo(other: ChangeTrustOperation): Matcher[ChangeTrustOperation] = beLike[ChangeTrustOperation] {
    case op =>
      op.limit must beEquivalentTo(other.limit)
  }

  def beEquivalentTo(other: CreateAccountOperation): Matcher[CreateAccountOperation] = beLike[CreateAccountOperation] {
    case op =>
      op.destinationAccount.accountId mustEqual other.destinationAccount.accountId
      op.startingBalance.units mustEqual other.startingBalance.units
  }

  def beEquivalentTo(other: CreatePassiveOfferOperation): Matcher[CreatePassiveOfferOperation] = beLike[CreatePassiveOfferOperation] {
    case op =>
      op.selling must beEquivalentTo(other.selling)
      op.buying must beEquivalentTo(other.buying)
      op.price mustEqual other.price
  }

  def beEquivalentTo(other: DeleteDataOperation): Matcher[DeleteDataOperation] = beLike[DeleteDataOperation] {
    case op =>
      op.name mustEqual other.name
  }

  def beEquivalentTo(other: WriteDataOperation): Matcher[WriteDataOperation] = beLike[WriteDataOperation] {
    case op =>
      op.name mustEqual other.name
      op.value.toSeq mustEqual other.value.toSeq
  }

  def beEquivalentTo(other: CreateOfferOperation): Matcher[CreateOfferOperation] = beLike[CreateOfferOperation] {
    case op =>
      op.selling must beEquivalentTo(other.selling)
      op.buying must beEquivalentTo(other.buying)
      op.price mustEqual other.price
  }

  def beEquivalentTo(other: DeleteOfferOperation): Matcher[DeleteOfferOperation] = beLike[DeleteOfferOperation] {
    case op =>
      op.offerId mustEqual other.offerId
      op.selling must beEquivalentTo(other.selling)
      op.buying must beEquivalentTo(other.buying)
      op.price mustEqual other.price
  }

  def beEquivalentTo(other: UpdateOfferOperation): Matcher[UpdateOfferOperation] = beLike[UpdateOfferOperation] {
    case op =>
      op.offerId mustEqual other.offerId
      op.selling must beEquivalentTo(other.selling)
      op.buying must beEquivalentTo(other.buying)
      op.price mustEqual other.price
  }

  def beEquivalentTo(other: PathPaymentOperation): Matcher[PathPaymentOperation] = beLike[PathPaymentOperation] {
    case op =>
      op.sendMax must beEquivalentTo(other.sendMax)
      op.destinationAccount.accountId mustEqual other.destinationAccount.accountId
      op.destinationAmount must beEquivalentTo(other.destinationAmount)
      forall(op.path.zip(other.path)) {
        case (expected: Asset, actual: Asset) => actual must beEquivalentTo(expected)
      }
  }

  def beEquivalentTo(other: PaymentOperation): Matcher[PaymentOperation] = beLike[PaymentOperation] {
    case op =>
      op.destinationAccount.accountId mustEqual other.destinationAccount.accountId
      op.amount must beEquivalentTo(other.amount)
  }

  def beEquivalentTo(other: SetOptionsOperation): Matcher[SetOptionsOperation] = beLike[SetOptionsOperation] {
    case op =>
      op.clearFlags mustEqual other.clearFlags
      op.highThreshold mustEqual other.highThreshold
      op.homeDomain mustEqual other.homeDomain
      op.inflationDestination.map(_.accountId) mustEqual other.inflationDestination.map(_.accountId)
      op.lowThreshold mustEqual other.lowThreshold
      op.masterKeyWeight mustEqual other.masterKeyWeight
      op.mediumThreshold mustEqual other.mediumThreshold
      op.signer mustEqual other.signer
  }

  def beEquivalentTo[T <: Operation](other: T): Matcher[T] = beLike {
    case op: AccountMergeOperation => other.asInstanceOf[AccountMergeOperation] must beEquivalentTo(op)
    case op: AllowTrustOperation => other.asInstanceOf[AllowTrustOperation] must beEquivalentTo(op)
    case op: ChangeTrustOperation => other.asInstanceOf[ChangeTrustOperation] must beEquivalentTo(op)
    case op: CreateAccountOperation => other.asInstanceOf[CreateAccountOperation] must beEquivalentTo(op)
    case op: CreatePassiveOfferOperation => other.asInstanceOf[CreatePassiveOfferOperation] must beEquivalentTo(op)
    case op: DeleteDataOperation => other.asInstanceOf[DeleteDataOperation] must beEquivalentTo(op)
    case op: WriteDataOperation => other.asInstanceOf[WriteDataOperation] must beEquivalentTo(op)
    case op: CreateOfferOperation => other.asInstanceOf[CreateOfferOperation] must beEquivalentTo(op)
    case op: DeleteOfferOperation => other.asInstanceOf[DeleteOfferOperation] must beEquivalentTo(op)
    case op: UpdateOfferOperation => other.asInstanceOf[UpdateOfferOperation] must beEquivalentTo(op)
    case op: PathPaymentOperation => other.asInstanceOf[PathPaymentOperation] must beEquivalentTo(op)
    case op: PaymentOperation => other.asInstanceOf[PaymentOperation] must beEquivalentTo(op)
    case op: SetOptionsOperation => other.asInstanceOf[SetOptionsOperation] must beEquivalentTo(op)
    case op => other mustEqual op
  }

  def beEquivalentTo(other: Account): Matcher[Account] = beLike {
    case acc =>
      acc.publicKey must beEquivalentTo(other.publicKey)
      acc.sequenceNumber mustEqual other.sequenceNumber
  }

  def beEquivalentTo(other: Transaction): Matcher[Transaction] = beLike {
    case txn =>
      txn.source must beEquivalentTo(other.source)
      txn.memo must beEquivalentTo(other.memo)
      txn.timeBounds mustEqual other.timeBounds
      txn.hash mustEqual other.hash
      forall(txn.operations.zip(other.operations)) {
        case (txnOp: Operation, otherOp: Operation) =>
          txnOp must beEquivalentTo(otherOp)
      }
  }

  def beEquivalentTo(other: SignedTransaction): Matcher[SignedTransaction] = beLike {
    case stxn =>
      stxn.transaction must beEquivalentTo(other.transaction)
      forall(stxn.signatures.zip(other.signatures)) {
        case (stxnSig: Signature, otherSig: Signature) =>
          stxnSig.hint.toSeq mustEqual otherSig.hint.toSeq
          stxnSig.data.toSeq mustEqual otherSig.data.toSeq
      }
  }

  def beEquivalentTo(other: Memo): Matcher[Memo] = beLike[Memo] {
    case memo =>
      (memo, other) match {
        case (MemoHash(a), MemoHash(b)) => base64(a) mustEqual base64(b)
        case (MemoReturnHash(a), MemoReturnHash(b)) => a.toSeq mustEqual b.toSeq
        case _ => memo mustEqual other
      }
  }

  def beEquivalentTo(other: TransactionHistory): Matcher[TransactionHistory] = beLike {
    case thr =>
      other.memo must beEquivalentTo(thr.memo)
      other.copy(memo = thr.memo) mustEqual thr
  }

}

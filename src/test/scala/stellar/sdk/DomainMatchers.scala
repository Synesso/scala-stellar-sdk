package stellar.sdk

import cats.data.State
import org.apache.commons.codec.binary.Hex
import org.specs2.matcher.{AnyMatchers, Matcher, MustExpectations, OptionMatchers, SequenceMatchersCreation}
import stellar.sdk.model._
import stellar.sdk.util.ByteArrays.base64
import stellar.sdk.model.op._
import stellar.sdk.model.result.TransactionHistory
import stellar.sdk.model.xdr.Encodable

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

  def beEquivalentTo(other: Signer): Matcher[Signer] = beLike[Signer] {
    case Signer(key, weight) =>
        weight mustEqual other.weight
        key must beEquivalentTo(other.key)
  }

  def beEquivalentTo(key: StrKey): Matcher[StrKey] = beLike[StrKey] {
    case Seed(hash) if key.isInstanceOf[Seed] => hash mustEqual key.hash
    case other => other.asInstanceOf[SignerStrKey] must beEquivalentTo(key.asInstanceOf[SignerStrKey])
  }

  def beEquivalentTo(key: SignerStrKey): Matcher[SignerStrKey] = beLike[SignerStrKey] {
    case AccountId(hash, subAccountId) if key.isInstanceOf[AccountId] =>
      hash mustEqual key.hash
      subAccountId mustEqual key.asInstanceOf[AccountId].subAccountId
    case PreAuthTx(hash) if key.isInstanceOf[PreAuthTx]  => hash mustEqual key.hash
    case SHA256Hash(hash) if key.isInstanceOf[SHA256Hash] => hash mustEqual key.hash
  }

  def beEquivalentTo(other: Transacted[Operation]): Matcher[Transacted[Operation]] = beLike[Transacted[Operation]] {
    case t =>
      t.copy(operation = other.operation) mustEqual other
      t.operation must beEquivalentTo(other.operation)
  }

  def beEquivalentTo(other: AccountMergeOperation): Matcher[AccountMergeOperation] = beLike[AccountMergeOperation] {
    case op =>
      op.destination mustEqual other.destination
  }

  def beEquivalentTo(other: AllowTrustOperation): Matcher[AllowTrustOperation] = beLike[AllowTrustOperation] {
    case op =>
      op.trustor.accountId mustEqual other.trustor.accountId
      op.assetCode mustEqual other.assetCode
      op.trustLineFlags mustEqual other.trustLineFlags
  }

  def beEquivalentTo(other: ChangeTrustOperation): Matcher[ChangeTrustOperation] = beLike[ChangeTrustOperation] {
    case op =>
      op.limit must beEquivalentTo(other.limit)
  }

  def beEquivalentTo(other: CreateAccountOperation): Matcher[CreateAccountOperation] = beLike[CreateAccountOperation] {
    case op =>
      op.destinationAccount mustEqual other.destinationAccount
      op.startingBalance.units mustEqual other.startingBalance.units
  }

  def beEquivalentTo(other: CreatePassiveSellOfferOperation): Matcher[CreatePassiveSellOfferOperation] = beLike[CreatePassiveSellOfferOperation] {
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

  def beEquivalentTo(other: CreateSellOfferOperation): Matcher[CreateSellOfferOperation] = beLike[CreateSellOfferOperation] {
    case op =>
      op.selling must beEquivalentTo(other.selling)
      op.buying must beEquivalentTo(other.buying)
      op.price mustEqual other.price
  }

  def beEquivalentTo(other: DeleteSellOfferOperation): Matcher[DeleteSellOfferOperation] = beLike[DeleteSellOfferOperation] {
    case op =>
      op.offerId mustEqual other.offerId
      op.selling must beEquivalentTo(other.selling)
      op.buying must beEquivalentTo(other.buying)
      op.price mustEqual other.price
  }

  def beEquivalentTo(other: UpdateSellOfferOperation): Matcher[UpdateSellOfferOperation] = beLike[UpdateSellOfferOperation] {
    case op =>
      op.offerId mustEqual other.offerId
      op.selling must beEquivalentTo(other.selling)
      op.buying must beEquivalentTo(other.buying)
      op.price mustEqual other.price
  }

  def beEquivalentTo(other: PathPaymentStrictReceiveOperation): Matcher[PathPaymentStrictReceiveOperation] = beLike[PathPaymentStrictReceiveOperation] {
    case op =>
      op.sendMax must beEquivalentTo(other.sendMax)
      op.destinationAccount mustEqual other.destinationAccount
      op.destinationAmount must beEquivalentTo(other.destinationAmount)
      forall(op.path.zip(other.path)) {
        case (expected: Asset, actual: Asset) => actual must beEquivalentTo(expected)
      }
  }

  def beEquivalentTo(other: PathPaymentStrictSendOperation): Matcher[PathPaymentStrictSendOperation] = beLike[PathPaymentStrictSendOperation] {
    case op =>
      op.destinationMin must beEquivalentTo(other.destinationMin)
      op.destinationAccount mustEqual other.destinationAccount
      op.sendAmount must beEquivalentTo(other.sendAmount)
      forall(op.path.zip(other.path)) {
        case (expected: Asset, actual: Asset) => actual must beEquivalentTo(expected)
      }
  }

  def beEquivalentTo(other: PaymentOperation): Matcher[PaymentOperation] = beLike[PaymentOperation] {
    case op =>
      op.destinationAccount mustEqual other.destinationAccount
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
      op.signer match {
        case Some(s) =>
          other.signer must beSome[Signer].like {
            case otherS => s must beEquivalentTo(otherS)
          }
        case None => other.signer must beNone
      }
  }

  def beEquivalentTo[T <: Operation](other: T): Matcher[T] = beLike {
    case op: AccountMergeOperation => other.asInstanceOf[AccountMergeOperation] must beEquivalentTo(op)
    case op: AllowTrustOperation => other.asInstanceOf[AllowTrustOperation] must beEquivalentTo(op)
    case op: ChangeTrustOperation => other.asInstanceOf[ChangeTrustOperation] must beEquivalentTo(op)
    case op: CreateAccountOperation => other.asInstanceOf[CreateAccountOperation] must beEquivalentTo(op)
    case op: CreatePassiveSellOfferOperation => other.asInstanceOf[CreatePassiveSellOfferOperation] must beEquivalentTo(op)
    case op: DeleteDataOperation => other.asInstanceOf[DeleteDataOperation] must beEquivalentTo(op)
    case op: WriteDataOperation => other.asInstanceOf[WriteDataOperation] must beEquivalentTo(op)
    case op: CreateSellOfferOperation => other.asInstanceOf[CreateSellOfferOperation] must beEquivalentTo(op)
    case op: DeleteSellOfferOperation => other.asInstanceOf[DeleteSellOfferOperation] must beEquivalentTo(op)
    case op: UpdateSellOfferOperation => other.asInstanceOf[UpdateSellOfferOperation] must beEquivalentTo(op)
    case op: PathPaymentStrictReceiveOperation => other.asInstanceOf[PathPaymentStrictReceiveOperation] must beEquivalentTo(op)
    case op: PathPaymentStrictSendOperation => other.asInstanceOf[PathPaymentStrictSendOperation] must beEquivalentTo(op)
    case op: PaymentOperation => other.asInstanceOf[PaymentOperation] must beEquivalentTo(op)
    case op: SetOptionsOperation => other.asInstanceOf[SetOptionsOperation] must beEquivalentTo(op)
    case op => other mustEqual op
  }

  def beEquivalentTo(other: PaymentSigningRequest): Matcher[PaymentSigningRequest] = beLike {
    case psr =>
      psr.destination mustEqual other.destination
      psr.amount mustEqual other.amount
  }

  def beEquivalentTo(other: TransactionSigningRequest): Matcher[TransactionSigningRequest] = beLike {
    case tsr =>
      tsr.transaction must beEquivalentTo(other.transaction)
      tsr.form mustEqual other.form
      tsr.callback mustEqual other.callback
      tsr.pubkey mustEqual other.pubkey
      tsr.message mustEqual other.message
      tsr.networkPassphrase mustEqual other.networkPassphrase
      tsr.signature mustEqual other.signature
  }

  def beEquivalentTo(other: Account): Matcher[Account] = beLike {
    case acc =>
      acc.id must beEquivalentTo(other.id)
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

  def serdeUsing[E <: Encodable](decoder: State[Seq[Byte], E]): Matcher[E] = beLike {
    case expected: Encodable =>
      val encoded = expected.encode.toList
      val (remaining, actual) = decoder.run(encoded).value
      actual mustEqual expected
      remaining must beEmpty
  }
}

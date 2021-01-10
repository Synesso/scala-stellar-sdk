package stellar.sdk.model

import okhttp3.HttpUrl
import okio.ByteString
import org.stellar.xdr.EnvelopeType.{ENVELOPE_TYPE_TX, ENVELOPE_TYPE_TX_FEE_BUMP}
import org.stellar.xdr.{EnvelopeType, FeeBumpTransaction, FeeBumpTransactionEnvelope, Int64, SequenceNumber, TransactionEnvelope, TransactionV0Envelope, TransactionV1Envelope, Uint32, Transaction => XTransaction}
import stellar.sdk.model.op.{CreateAccountOperation, Operation}
import stellar.sdk.model.response.TransactionPostResponse
import stellar.sdk.util.ByteArrays
import stellar.sdk.{KeyPair, Network, PublicKey, PublicKeyOps, PublicNetwork, Signature}

import scala.concurrent.{ExecutionContext, Future}

case class Transaction(
  source: Account,
  operations: Seq[Operation] = Nil,
  memo: Memo = NoMemo,
  timeBounds: TimeBounds,
  maxFee: NativeAmount,
  overrideMemoRequirement: Boolean = false
)(implicit val network: Network) {
  private val BaseFee = 100L

  def add(op: Operation): Transaction = this.copy(operations = operations :+ op)

  def minFee: NativeAmount = NativeAmount(operations.size * BaseFee)

  def sign(key: KeyPair, otherKeys: KeyPair*): SignedTransaction = {
    val h = hash.toArray
    val signatures = (key +: otherKeys).map(_.sign(h))
    SignedTransaction(this, signatures)
  }

  def sign(preImage: Seq[Byte]): SignedTransaction = {
    val signedPreImage = Signature(preImage.toArray, ByteArrays.sha256(preImage).drop(28))
    val signatures = List(signedPreImage)
    SignedTransaction(this, signatures)
  }

  def xdr: XTransaction = new XTransaction.Builder()
    .sourceAccount(source.id.muxedXdr)
    .fee(new Uint32(maxFee.units.toInt))
    .seqNum(new SequenceNumber(new Int64(source.sequenceNumber)))
    .timeBounds(timeBounds.xdr)
    .memo(memo.xdr)
    .operations(operations.map(_.xdr).toArray)
    .ext(new XTransaction.TransactionExt.Builder().discriminant(0).build())
    .build()

  def hash: Seq[Byte] = ByteArrays.sha256(network.networkId ++ xdr.encode().toByteArray)
    .toIndexedSeq

  /** The `web+stellar:` URL for this transaction. */
  def signingRequest: TransactionSigningRequest = TransactionSigningRequest(
    transaction = SignedTransaction(this, Nil),
    networkPassphrase = Some(network).filterNot(_ == PublicNetwork).map(_.passphrase)
  )

  /**
   * If the transaction has no memo, these are the payment destination accounts that must be OK with not receiving
   * a memo.
   */
  def payeeAccounts: List[AccountId] = operations.toList.flatMap(_.accountRequiringMemo)

  def encodeXDR: String = xdr.encode().base64()

}

object Transaction {
  def decode(xdr: XTransaction)(implicit network: Network): Transaction = Transaction(
    source = ???,
    operations = ???,
    memo = ???,
    timeBounds = ???,
    maxFee = ???,
    overrideMemoRequirement = ???
  )

  def decodeXDR(envelopeXDR: String)(implicit network: Network): Transaction = ???
}

case class SignedTransaction(
  transaction: Transaction,
  signatures: Seq[Signature],
  feeBump: Option[FeeBump] = None
) {
  assert(transaction.minFee.units <= transaction.maxFee.units,
    "Insufficient maxFee. Allow at least 100 stroops per operation. " +
      s"[maxFee=${transaction.maxFee.units}, operations=${transaction.operations.size}].")

  def submit()(implicit ec: ExecutionContext): Future[TransactionPostResponse] = {
    transaction.network.submit(this)
  }

  def sign(key: KeyPair): SignedTransaction =
    this.copy(signatures = key.sign(transaction.hash.toArray) +: signatures)

  def sign(preImage: Seq[Byte]): SignedTransaction =
    this.copy(signatures = Signature(preImage.toArray, ByteArrays.sha256(preImage)) +: signatures)

  /**
   * Returns true if any of the signatures are valid for this transaction and signed by the account indicated by the
   * `key` parameter.
   *
   * @param key the account to test
   */
  def verify(key: PublicKeyOps): Boolean = {
    signatures.exists(signature => key.verify(transaction.hash.toArray, signature.data))
  }

  def hasMemo = transaction.memo != NoMemo

  /**
   * The base64 encoding of the XDR form of this signed transaction.
   */
  def encodeXDR: String = xdr.encode.hex()

  def xdr: TransactionEnvelope = {
    val transactionXdr = new TransactionV1Envelope.Builder()
      .tx(transaction.xdr)
      .signatures(signatures.map(_.xdr).toArray)
      .build()
    val builder = new TransactionEnvelope.Builder()
    feeBump match {
      case None =>
        builder
          .discriminant(ENVELOPE_TYPE_TX)
          .v1(transactionXdr)
      case Some(bump) =>
        builder
          .discriminant(ENVELOPE_TYPE_TX_FEE_BUMP)
          .feeBump(new FeeBumpTransactionEnvelope.Builder()
          .tx(new FeeBumpTransaction.Builder()
            .fee(new Int64(bump.fee.units))
            .feeSource(bump.source.muxedXdr)
            .innerTx(new FeeBumpTransaction.FeeBumpTransactionInnerTx.Builder()
              .discriminant(ENVELOPE_TYPE_TX)
              .v1(transactionXdr)
              .build())
            .build())
          .build())
    }
    builder.build()
  }

  /** The `web+stellar:` URL for this transaction. */
  def signingRequest(
    form: Map[String, (String, String)] = Map.empty,
    callback: Option[HttpUrl] = None,
    pubkey: Option[PublicKey] = None,
    message: Option[String] = None,
    networkPassphrase: Option[String] = None,
    requestSigner: Option[KeyPair] = None
  ): TransactionSigningRequest = {
    val tsr = TransactionSigningRequest(this, form, callback, pubkey, message, networkPassphrase)
    requestSigner.map(tsr.sign("foo.com:", _)).getOrElse(tsr)
  }

  /** Bump a signed transaction with a bigger fee */
  def bumpFee(fee: NativeAmount, source: KeyPair): SignedTransaction = {
    val encodedFeeBump = transaction.copy(
      source = Account(source.toAccountId, 0L),
      maxFee = fee
    )(transaction.network).xdr.encode().sha256().toByteArray
    val signature = source.sign(encodedFeeBump)
    val feeBump = FeeBump(source.toAccountId, fee, List(signature))
    this.copy(feeBump = Some(feeBump))
  }

  def payeeAccounts: List[AccountId] = transaction.payeeAccounts

  def createdAccounts: List[AccountId] = transaction.operations.toList.flatMap {
    case CreateAccountOperation(destination, _, _) => Some(destination)
    case _ => None
  }
}

object SignedTransaction {

  /**
   * Decodes a signed transaction (aka envelope) from base64-encoded XDR.
   */
  def decode(bs: ByteString)(implicit network: Network): SignedTransaction = {
    val decoded = TransactionEnvelope.decode(bs)
    decoded.getDiscriminant match {
      case EnvelopeType.ENVELOPE_TYPE_TX_V0 =>
        val envelope = TransactionV0Envelope.decode(bs)
        val tx = envelope.getTx
        SignedTransaction(
          transaction = Transaction(
            source = Account(
              id = AccountId(tx.getSourceAccountEd25519.getUint256),
              sequenceNumber = tx.getSeqNum.getSequenceNumber.getInt64
            ),
            operations = tx.getOperations.map(Operation.decode),
            // TODO - From here!
            memo = ???,
            timeBounds = ???,
            maxFee = ???,
            overrideMemoRequirement = ???
          ),
          signatures = List(???),
          feeBump = Some(???)
        )

      case EnvelopeType.ENVELOPE_TYPE_TX =>
        val envelope = TransactionV1Envelope.decode(bs)
        SignedTransaction(
          transaction = Transaction(
            source = ???,
            operations = ???,
            memo = ???,
            timeBounds = ???,
            maxFee = ???,
            overrideMemoRequirement = ???
          ),
          signatures = List(???),
          feeBump = Some(???)
        )

      case EnvelopeType.ENVELOPE_TYPE_TX_FEE_BUMP =>
        val envelope = FeeBumpTransactionEnvelope.decode(bs)
        SignedTransaction(
          transaction = Transaction(
            source = ???,
            operations = ???,
            memo = ???,
            timeBounds = ???,
            maxFee = ???,
            overrideMemoRequirement = ???
          ),
          signatures = List(???),
          feeBump = Some(???)
        )

      case _ => throw new IllegalArgumentException(s"Cannot decode ${decoded.getDiscriminant} into a SignedTransaction")
    }
  }

  /**
   * Decodes a signed transaction (aka envelope) from base64-encoded XDR.
   */
  def decodeXDR(base64: String)(implicit network: Network): SignedTransaction =
    decode(ByteString.decodeBase64(base64))

}

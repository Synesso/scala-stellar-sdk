package stellar.sdk.model

import java.net.{URI, URLEncoder}

import cats.data._
import okhttp3.HttpUrl
import stellar.sdk.model.TimeBounds.Unbounded
import stellar.sdk.model.op.Operation
import stellar.sdk.model.response.TransactionPostResponse
import stellar.sdk.model.xdr.Encode.{arr, int, long, opt}
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays
import stellar.sdk.util.ByteArrays._
import stellar.sdk.{KeyPair, Network, Signature}

import scala.concurrent.{ExecutionContext, Future}

case class Transaction(source: Account,
                       operations: Seq[Operation] = Nil,
                       memo: Memo = NoMemo,
                       timeBounds: TimeBounds,
                       maxFee: NativeAmount)(implicit val network: Network) extends Encodable {

  private val BaseFee = 100L
  private val EnvelopeTypeTx = 2

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

  def hash: Seq[Byte] = ByteArrays.sha256(network.networkId ++ encode)
    .toIndexedSeq

  /** The `web+stellar:` URL for this transaction. */
  def signingRequest: TransactionSigningRequest = TransactionSigningRequest(SignedTransaction(this, Nil))

  /**
    * The base64 encoding of the XDR form of this unsigned transaction.
    */
  def encodeXDR: String = base64(encode)

  // Encodes to TransactionV0 format by default for backwards compatibility with core protocol 12.
  // But if the accountId is muxed (has a sub-account id), then encode to TransactionV1 for protocol 13+
  def encode: LazyList[Byte] = encodeV1

  def encodeV0: LazyList[Byte] = {
    source.id.copy(subAccountId = None).encode ++
      int(maxFee.units.toInt) ++
      long(source.sequenceNumber) ++
      opt(Some(timeBounds).filterNot(_ == Unbounded)) ++
      memo.encode ++
      arr(operations) ++
      int(0)
  }

  def encodeV1: LazyList[Byte] = {
    int(2) ++
      source.id.encode ++
      int(maxFee.units.toInt) ++
      long(source.sequenceNumber) ++
      opt(Some(timeBounds).filterNot(_ == Unbounded)) ++
      memo.encode ++
      arr(operations) ++
      int(0)
  }
}

object Transaction extends Decode {

  /**
    * Decodes an unsigned transaction from base64-encoded XDR.
    */
  def decodeXDR(base64: String)(implicit network: Network): Transaction =
    decode.run(ByteArrays.base64(base64).toIndexedSeq).value._2

  def decode(implicit network: Network): State[Seq[Byte], Transaction] = int.flatMap {
    case 0 => decodeV0
    case 2 => decodeV1
  }

  def decodeV0(implicit network: Network): State[Seq[Byte], Transaction] = {
    for {
      publicKeyBytes <- bytes(32)
      accountId = AccountId(publicKeyBytes.toArray[Byte])
      fee <- int
      seqNo <- long
      timeBounds <- opt(TimeBounds.decode).map(_.getOrElse(Unbounded))
      memo <- Memo.decode
      ops <- arr(Operation.decode)
      _ <- int
    } yield Transaction(Account(accountId, seqNo), ops, memo, timeBounds, NativeAmount(fee))
  }

  def decodeV1(implicit network: Network): State[Seq[Byte], Transaction] = {
    for {
      accountId <- AccountId.decode
      fee <- int
      seqNo <- long
      timeBounds <- opt(TimeBounds.decode).map(_.getOrElse(Unbounded))
      memo <- Memo.decode
      ops <- arr(Operation.decode)
      _ <- int
    } yield Transaction(Account(accountId, seqNo), ops, memo, timeBounds, NativeAmount(fee))
  }
}

case class SignedTransaction(transaction: Transaction,
                             signatures: Seq[Signature],
                             feeBump: Option[FeeBump] = None) {

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
    * The base64 encoding of the XDR form of this signed transaction.
    */
  def encodeXDR: String = base64(encode)

  def encode: LazyList[Byte] = feeBump.map(encodeFeeBump)
    .getOrElse(transaction.encode ++ arr(signatures))

  /** The `web+stellar:` URL for this transaction. */
  def signingRequest: TransactionSigningRequest = TransactionSigningRequest(this)

  private def encodeFeeBump(bump: FeeBump): LazyList[Byte] =
    int(5) ++
      bump.source.encode ++
      int(bump.fee.units.toInt) ++
      transaction.encode ++
      arr(signatures) ++
      int(0) ++
      arr(bump.signatures)
}

object SignedTransaction extends Decode {

  /**
    * Decodes a signed transaction (aka envelope) from base64-encoded XDR.
    */
  def decodeXDR(base64: String)(implicit network: Network): SignedTransaction =
    decode.run(ByteArrays.base64(base64).toIndexedSeq).value._2

  def decode(implicit network: Network): State[Seq[Byte], SignedTransaction] = for {
    discriminator <- int
    txn <- discriminator match {
      // parse a legacy transaction, without the public key discriminator, into a standard transaction
      case 0 => Transaction.decodeV0.map(Left(_))

      // parse a standard transaction, with MuxedAccount
      case 2 => Transaction.decodeV1.map(Left(_))

      // parse a fee bump transaction
      case 5 =>  for {
        accountId <- AccountId.decode
        fee <- int
        _ <- int // 2
        transaction <- Transaction.decodeV1
        transactionSigs <- arr(Signature.decode)
        _ <- int // 0
        feeBump = Some(FeeBump(accountId, NativeAmount(fee), Nil))
      } yield Right(SignedTransaction(transaction, transactionSigs, feeBump))
    }
    sigs <- arr(Signature.decode).map(_.toList)
  } yield txn match {
    case Left(t: Transaction) => SignedTransaction(t, sigs)
    case Right(st: SignedTransaction) => st.copy(feeBump = st.feeBump.map(_.copy(signatures = sigs)))
  }
}

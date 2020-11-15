package stellar.sdk.model

import cats.data._
import okhttp3.HttpUrl
import stellar.sdk.model.TimeBounds.Unbounded
import stellar.sdk.model.op.{CreateAccountOperation, Operation}
import stellar.sdk.model.response.TransactionPostResponse
import stellar.sdk.model.xdr.Encode.{arr, bytes, int, long, opt}
import stellar.sdk.model.xdr.{Decode, Encodable}
import stellar.sdk.util.ByteArrays
import stellar.sdk.util.ByteArrays._
import stellar.sdk.{KeyPair, Network, PublicKey, PublicKeyOps, PublicNetwork, Signature}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class Transaction(source: Account,
                       operations: Seq[Operation] = Nil,
                       memo: Memo = NoMemo,
                       timeBounds: TimeBounds,
                       maxFee: NativeAmount)(implicit val network: Network) extends Encodable {

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

  def hash: Seq[Byte] = ByteArrays.sha256(network.networkId ++ encode)
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

  /**
    * The base64 encoding of the XDR form of this unsigned transaction.
    */
  def encodeXDR: String = base64(encode)

  // Encodes to TransactionV1 format by default
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
   * Returns true if any of the signatures are valid for this transaction and signed by the account indicated by the
   * `key` parameter.
   * @param key the account to test
   */
  def verify(key: PublicKeyOps): Boolean = {
    signatures.exists(signature => key.verify(transaction.hash.toArray, signature.data))
  }

  def hasMemo = transaction.memo != NoMemo

  /**
    * The base64 encoding of the XDR form of this signed transaction.
    */
  def encodeXDR: String = base64(encode)

  def encode: LazyList[Byte] = feeBump.map(encodeFeeBump)
    .getOrElse(transaction.encode ++ arr(signatures))

  def encodeV0: LazyList[Byte] = transaction.encodeV0 ++ arr(signatures)

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
    val encodedFeeBump = ByteArrays.sha256(encodeFeeBumpBase(source.toAccountId, fee))
    val signature = source.sign(encodedFeeBump)
    val feeBump = FeeBump(source.toAccountId, fee, List(signature))
    this.copy(feeBump = Some(feeBump))
  }

  def payeeAccounts: List[AccountId] = transaction.payeeAccounts
  def createdAccounts: List[AccountId] = transaction.operations.toList.flatMap {
    case CreateAccountOperation(destination, _, _) => Some(destination)
    case _ => None
  }

  private def encodeFeeBumpBase(accountId: AccountId, fee: NativeAmount): LazyList[Byte] = {
    bytes(32, transaction.network.networkId) ++
      int(5) ++
      accountId.encode ++
      long(fee.units) ++
      transaction.encodeV1 ++
      arr(signatures) ++
      int(0)
  }

  private def encodeFeeBump(bump: FeeBump): LazyList[Byte] =
    int(5) ++
      bump.source.encode ++
      long(bump.fee.units) ++
      transaction.encodeV1 ++
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
        fee <- long
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

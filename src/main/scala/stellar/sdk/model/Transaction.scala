package stellar.sdk.model

import cats.data._
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

  def hash: Seq[Byte] = ByteArrays.sha256(network.networkId ++ Encode.int(EnvelopeTypeTx) ++ encode)

  /**
    * The base64 encoding of the XDR form of this unsigned transaction.
    */
  def encodeXDR: String = base64(encode)

  def encode: Stream[Byte] = {
    source.publicKey.encode ++
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
    decode.run(ByteArrays.base64(base64)).value._2

  def decode(implicit network: Network): State[Seq[Byte], Transaction] = for {
      publicKey <- KeyPair.decode
      fee <- int
      seqNo <- long
      timeBounds <- opt(TimeBounds.decode).map(_.getOrElse(Unbounded))
      memo <- Memo.decode
      ops <- arr(Operation.decode)
      _ <- int
    } yield {
    Transaction(Account(publicKey, seqNo), ops, memo, timeBounds, NativeAmount(fee))
  }
}

case class SignedTransaction(transaction: Transaction, signatures: Seq[Signature]) {

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

  def encode: Stream[Byte] = transaction.encode ++ Encode.arr(signatures)
}

object SignedTransaction extends Decode {

  /**
    * Decodes a signed transaction (aka envelope) from base64-encoded XDR.
    */
  def decodeXDR(base64: String)(implicit network: Network) =
    decode.run(ByteArrays.base64(base64)).value._2

  def decode(implicit network: Network): State[Seq[Byte], SignedTransaction] = for {
    txn <- Transaction.decode
    sigs <- arr(Signature.decode)
  } yield SignedTransaction(txn, sigs)

}

package stellar.sdk

import cats.data._
import stellar.sdk.ByteArrays._
import stellar.sdk.op.Operation
import stellar.sdk.response.TransactionPostResponse
import stellar.sdk.xdr.Encode.{arr, int, long, opt}
import stellar.sdk.xdr.{Decode, Encodable, Encode}

import scala.concurrent.{ExecutionContext, Future}

case class Transaction(source: Account,
                       operations: Seq[Operation] = Nil,
                       memo: Memo = NoMemo,
                       timeBounds: Option[TimeBounds] = None,
                       fee: Option[NativeAmount] = None)(implicit val network: Network) extends Encodable {

  private val BaseFee = 100L
  private val EnvelopeTypeTx = 2

  def add(op: Operation): Transaction = this.copy(operations = operations :+ op)

  /**
    * @return The maximum of
    *         A: The fee derived from the quantity of transactions; or
    *         B: the specified `fee`.
    */
  def calculatedFee: NativeAmount = {
    val minFee = BaseFee * operations.size
    NativeAmount(math.max(minFee, fee.map(_.units).getOrElse(minFee)))
  }

  def sign(key: KeyPair, otherKeys: KeyPair*): SignedTransaction = {
    val h = hash.toArray
    val signatures = (key +: otherKeys).map(_.sign(h))
    SignedTransaction(this, signatures)
  }

  def hash: Seq[Byte] = ByteArrays.sha256(network.networkId ++ Encode.int(EnvelopeTypeTx) ++ encode)

  /**
    * The base64 encoding of the XDR form of this unsigned transaction.
    */
  def encodeXDR: String = base64(encode)

  def encode: Stream[Byte] = {
    source.publicKey.encode ++
      int(calculatedFee.units.toInt) ++
      long(source.sequenceNumber) ++
      opt(timeBounds) ++
      memo.encode ++
      arr(operations) ++
      int(0)
  }
}

object Transaction {

  // todo - sometimes it's `decodeXDR` and sometimes `from`. One or the other.

  /**
    * Decodes an unsigned transaction from base64-encoded XDR.
    */
  def decodeXDR(base64: String)(implicit network: Network): Transaction =
    decode.run(ByteArrays.base64(base64)).value._2

  def decode(implicit network: Network): State[Seq[Byte], Transaction] = for {
      publicKey <- KeyPair.decode
      fee <- Decode.int
      seqNo <- Decode.long
      timeBounds <- Decode.opt(TimeBounds.decode)
      memo <- Memo.decode
      ops <- Decode.arr(Operation.decode)
      _ <- Decode.int
    } yield {
    Transaction(Account(publicKey, seqNo), ops, memo, timeBounds, Some(NativeAmount(fee)))
  }
}

case class SignedTransaction(transaction: Transaction, signatures: Seq[Signature]) {

  def submit()(implicit ec: ExecutionContext): Future[TransactionPostResponse] = {
    transaction.network.submit(this)
  }

  def sign(key: KeyPair): SignedTransaction =
    this.copy(signatures = key.sign(transaction.hash.toArray) +: signatures)

  /**
    * The base64 encoding of the XDR form of this signed transaction.
    */
  def encodeXDR: String = base64(encode)

  def encode: Stream[Byte] = transaction.encode ++ Encode.arr(signatures)
}

object SignedTransaction {

  /**
    * Decodes a signed transaction (aka envelope) from base64-encoded XDR.
    */
  def decodeXDR(base64: String)(implicit network: Network) =
    decode.run(ByteArrays.base64(base64)).value._2

  def decode(implicit network: Network): State[Seq[Byte], SignedTransaction] = for {
    txn <- Transaction.decode
    sigs <- Decode.arr(Signature.decode)
  } yield SignedTransaction(txn, sigs)

}

package stellar.sdk

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import org.stellar.sdk.xdr.Transaction.TransactionExt
import org.stellar.sdk.xdr.{DecoratedSignature, EnvelopeType, TransactionEnvelope, XdrDataOutputStream, Transaction => XDRTransaction}
import stellar.sdk.ByteArrays._
import stellar.sdk.XDRPrimitives._
import stellar.sdk.op.Operation
import stellar.sdk.resp.TransactionPostResp

import scala.concurrent.{ExecutionContext, Future}

case class Transaction(source: Account,
                       operations: Seq[Operation] = Nil,
                       memo: Memo = NoMemo,
                       timeBounds: Option[TimeBounds] = None,
                       fee: Option[NativeAmount] = None)(implicit val network: Network) extends Encodable {

  private val BaseFee = 100L
  private val EnvelopeTypeTx = ByteBuffer.allocate(4).putInt(EnvelopeType.ENVELOPE_TYPE_TX.getValue).array

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
    val h = hash
    val signatures = (key +: otherKeys).map(_.signToXDR(h))
    SignedTransaction(this, signatures, h)
  }

  def hash: Array[Byte] = sha256 {
    val os = new ByteArrayOutputStream
    os.write(network.networkId)
    os.write(EnvelopeTypeTx)
    val txOs = new ByteArrayOutputStream
    val xdrOs = new XdrDataOutputStream(txOs)
    XDRTransaction.encode(xdrOs, toXDR)
    os.write(txOs.toByteArray)
    os.toByteArray
  }

  def toXDR = {
    val txn = new XDRTransaction
    val ext = new TransactionExt
    ext.setDiscriminant(0)
    txn.setExt(ext)
    txn.setFee(uint32(calculatedFee.units.toInt))
    txn.setSeqNum(seqNum(source.sequenceNumber))
    txn.setSourceAccount(accountId(source.publicKey))
    txn.setOperations(operations.toArray.map(_.toXDR))
    txn.setMemo(memo.toXDR)
    timeBounds.map(_.toXDR).foreach(txn.setTimeBounds)
    txn
  }

  /**
    * The base64 encoding of the XDR form of this unsigned transaction.
    */
  def encodeXDR: String = {
    val baos = new ByteArrayOutputStream
    val os = new XdrDataOutputStream(baos)
    org.stellar.sdk.xdr.Transaction.encode(os, toXDR)
    base64(baos.toByteArray)
  }

  override def encode: Stream[Byte] = {
    source.encode ++
      calculatedFee.encode ++
      Encode.long(source.sequenceNumber) ++
      Encode.opt(timeBounds) ++
      memo.encode ++
      Encode.varArr(operations) ++
      Encode.int(0)
  }
}

object Transaction {

  def fromXDR(txn: XDRTransaction)(implicit network: Network): Transaction = {
    val account = Account(
      KeyPair.fromXDRPublicKey(txn.getSourceAccount.getAccountID),
      txn.getSeqNum.getSequenceNumber.getInt64
    )
    val operations = TrySeq.sequence(txn.getOperations.map(Operation.fromXDR)).get
    val memo = Memo.fromXDR(txn.getMemo)
    val timeBounds = Option(txn.getTimeBounds).map(TimeBounds.fromXDR)
    val fee = Some(NativeAmount(txn.getFee.getUint32.longValue.toInt))
    Transaction(account, operations, memo, timeBounds, fee)
  }

  /**
    * Decodes an unsigned transaction from base64-encoded XDR.
    */
  def decodeXDR(base64: String)(implicit network: Network): Transaction = {
    val xdrIn = XDRPrimitives.inputStream(base64)
    fromXDR(org.stellar.sdk.xdr.Transaction.decode(xdrIn))
  }
}

case class SignedTransaction(transaction: Transaction, signatures: Seq[DecoratedSignature], hash: Array[Byte]) {

  def submit()(implicit ec: ExecutionContext): Future[TransactionPostResp] = {
    transaction.network.submit(this)
  }

  def sign(key: KeyPair): SignedTransaction =
    this.copy(signatures = key.signToXDR(hash) +: signatures)

  def toXDR: TransactionEnvelope = {
    val xdr = new TransactionEnvelope
    xdr.setTx(transaction.toXDR)
    xdr.setSignatures(signatures.toArray)
    xdr
  }

  /**
    * The base64 encoding of the XDR form of this signed transaction.
    */
  def encodeXDR: String = {
    val baos = new ByteArrayOutputStream
    val os = new XdrDataOutputStream(baos)
    TransactionEnvelope.encode(os, toXDR)
    base64(baos.toByteArray)
  }
}

object SignedTransaction {

  /**
    * Decodes a signed transaction (aka envelope) from base64-encoded XDR.
    */
  def decodeXDR(base64: String)(implicit network: Network): SignedTransaction = {
    val xdrIn = XDRPrimitives.inputStream(base64)
    val envelope = TransactionEnvelope.decode(xdrIn)
    val txn = Transaction.fromXDR(envelope.getTx)
    val signatures = envelope.getSignatures
    val hash = txn.hash
    SignedTransaction(txn, signatures, hash)
  }

}

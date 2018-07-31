package stellar.sdk

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import org.stellar.sdk.xdr.Transaction.TransactionExt
import org.stellar.sdk.xdr.{DecoratedSignature, EnvelopeType, TransactionEnvelope, XdrDataOutputStream, Transaction => XDRTransaction}
import stellar.sdk.ByteArrays._
import stellar.sdk.TrySeq._
import stellar.sdk.XDRPrimitives._
import stellar.sdk.op.Operation
import stellar.sdk.resp.{AccountResp, TransactionPostResp}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class Transaction(source: Account,
                       operations: Seq[Operation] = Nil,
                       memo: Memo = NoMemo,
                       timeBounds: Option[TimeBounds] = None,
                       fee: Option[NativeAmount] = None)(implicit val network: Network) {

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
    for {
      h <- hash
      signatures <- sequence((key +: otherKeys).map(_.signToXDR(h)))
    } yield SignedTransaction(this, signatures, h)
  }.get

  def hash: Try[Array[Byte]] = sha256 {
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
}

object Transaction {

//  def apply(resp: AccountResp,
//            operations: Seq[Operation] = Nil,
//            memo: Memo = NoMemo,
//            timeBounds: Option[TimeBounds] = None,
//            fee: Option[NativeAmount] = None)(implicit network: Network): Transaction =
//    Transaction(resp.toAccount, operations, memo, timeBounds, fee)

  def fromXDR(txn: XDRTransaction)(implicit network: Network): Try[Transaction] = {
    val account = Account(
      KeyPair.fromXDRPublicKey(txn.getSourceAccount.getAccountID),
      txn.getSeqNum.getSequenceNumber.getUint64
    )
    for {
      operations <- TrySeq.sequence(txn.getOperations.map(Operation.fromXDR))
      memo = Memo.fromXDR(txn.getMemo)
      timeBounds = Option(txn.getTimeBounds).map(TimeBounds.fromXDR)
      fee = Some(NativeAmount(txn.getFee.getUint32.longValue.toInt))
    } yield Transaction(account, operations, memo, timeBounds, fee)
  }
}

case class SignedTransaction(transaction: Transaction, signatures: Seq[DecoratedSignature], hash: Array[Byte]) {

  def submit()(implicit ec: ExecutionContext): Future[TransactionPostResp] = {
    transaction.network.submit(this)
  }

  def sign(key: KeyPair): Try[SignedTransaction] = key.signToXDR(hash).map(sig =>
    this.copy(signatures = sig +: signatures)
  )

  def toEnvelopeXDR: TransactionEnvelope = {
    val xdr = new TransactionEnvelope
    xdr.setTx(transaction.toXDR)
    xdr.setSignatures(signatures.toArray)
    xdr
  }

  def toEnvelopeXDRBase64: Try[String] = Try {
    val baos = new ByteArrayOutputStream
    val os = new XdrDataOutputStream(baos)
    TransactionEnvelope.encode(os, toEnvelopeXDR)
    base64(baos.toByteArray)
  }
}

object SignedTransaction {

  def decodeXDR(base64: String)(implicit network: Network): Try[SignedTransaction] = {
    val xdrIn = XDRPrimitives.inputStream(base64)
    for {
      envelope <- Try(TransactionEnvelope.decode(xdrIn))
      txn <- Transaction.fromXDR(envelope.getTx)
      signatures = envelope.getSignatures
      hash <- txn.hash
    } yield SignedTransaction(txn, signatures, hash)
  }

}

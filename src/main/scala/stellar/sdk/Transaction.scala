package stellar.sdk

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import org.stellar.sdk.xdr.Transaction.TransactionExt
import org.stellar.sdk.xdr.{DecoratedSignature, EnvelopeType, TransactionEnvelope, XdrDataOutputStream, Transaction => XDRTransaction}
import stellar.sdk.ByteArrays._
import stellar.sdk.TrySeq._
import stellar.sdk.XDRPrimitives._
import stellar.sdk.op.Operation
import stellar.sdk.resp.TransactionResp

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class Transaction(source: Account,
                       operations: Seq[Operation] = Nil,
                       memo: Memo = NoMemo,
                       timeBounds: Option[TimeBounds] = None)(implicit val network: Network) {

  private val BaseFee = 100
  private val EnvelopeTypeTx = ByteBuffer.allocate(4).putInt(EnvelopeType.ENVELOPE_TYPE_TX.getValue).array

  def add(op: Operation): Transaction = this.copy(operations = operations :+ op)

  def sign(key: KeyPair, otherKeys: KeyPair*): Try[SignedTransaction] = for {
    h <- hash
    signatures <- sequence((key +: otherKeys).map(_.signToXDR(h)))
  } yield SignedTransaction(this, signatures, h)

  def fee: Int = BaseFee * operations.size

  private def hash: Try[Array[Byte]] = sha256 {
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
    txn.setFee(uint32(fee))
    txn.setSeqNum(seqNum(source.sequenceNumber))
    txn.setSourceAccount(accountId(source.keyPair))
    txn.setOperations(operations.toArray.map(_.toXDR))
    txn.setMemo(memo.toXDR)
    timeBounds.map(_.toXDR).foreach(txn.setTimeBounds)
    txn
  }
}

case class SignedTransaction(transaction: Transaction, signatures: Seq[DecoratedSignature], hash: Array[Byte]) {

  def submit()(implicit ec: ExecutionContext): Future[TransactionResp] = {
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


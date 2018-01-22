package stellar.scala.sdk

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import org.stellar.sdk.xdr.Transaction.TransactionExt
import org.stellar.sdk.xdr.{DecoratedSignature, EnvelopeType, XdrDataOutputStream, Transaction => XDRTransaction}
import stellar.scala.sdk.op.Operation
import stellar.scala.sdk.resp.SubmitTransactionResponse

import scala.util.Try

case class Transaction(source: Account,
                       memo: Memo,
                       operations: Seq[Operation] = Nil,
                       timeBounds: Option[TimeBounds] = None)(implicit val network: Network)
  extends ByteArrays with XDRPrimitives {

  private val BaseFee = 100
  private val EnvelopeTypeTx = ByteBuffer.allocate(4).putInt(EnvelopeType.ENVELOPE_TYPE_TX.getValue).array

  def add(op: Operation): Transaction = this.copy(operations = op +: operations)

  def sign(key: KeyPair, otherKeys: KeyPair*): Signed = Signed(Seq(???))

  def fee: Int = BaseFee * operations.size

  private def hash: Try[Array[Byte]] = sha256 {
    val os = new ByteArrayOutputStream
    os.write(network.networkId)
    os.write(EnvelopeTypeTx)
    val txOs = new ByteArrayOutputStream
    val xdrOs = new XdrDataOutputStream(txOs)
    XDRTransaction.encode(xdrOs, this.toXDR)
    os.write(txOs.toByteArray)
    os.toByteArray
  }

  private def toXDR = {
    val txn = new XDRTransaction
    val ext = new TransactionExt
    ext.setDiscriminant(0)
    txn.setFee(uint32(fee))
    txn.setSeqNum(seqNum(source.sequenceNumber))
    txn.setSourceAccount(accountId(source.keyPair))
    txn.setOperations(operations.reverse.toArray.map(_.toXDR))
    txn.setMemo(memo.toXDR)
    timeBounds.map(_.toXDR).foreach(txn.setTimeBounds)
    txn.setExt(ext)
    txn
  }

  case class Signed(signatures: Seq[DecoratedSignature]) {
    def submit(network: Network): SubmitTransactionResponse = ???

    def sign(key: KeyPair): Signed = this.copy(signatures = ??? +: signatures)
  }
}


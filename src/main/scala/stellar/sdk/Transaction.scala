package stellar.sdk

import cats.data._
import stellar.sdk.ByteArrays._
import stellar.sdk.op.Operation
import stellar.sdk.res.TransactionPostResponse
import stellar.sdk.xdr.Encode.{arr, int, long, opt}
import stellar.sdk.xdr.{Decode, Encode}

import scala.concurrent.{ExecutionContext, Future}

case class Transaction(source: Account,
                       operations: Seq[Operation] = Nil,
                       memo: Memo = NoMemo,
                       timeBounds: Option[TimeBounds] = None,
                       fee: Option[NativeAmount] = None)(implicit val network: Network) {

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
//      _ <- Decode.int.map{x => println(s"asdf $x") ; x}
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
  def encodeXDR: String = {
    val z =base64(encode)
//    println(z)
    z
  }

  /*

   todo - lets decode this txn bit by bit. :(
          or what happens if we decode automatically?

closest: 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
actual2: AAAAAHN2/eiOTNYcwPspSheGs/HQYfXy8cpXRl+qkyIRuUbWAAAFeAAAAAAAAAABAAAAAAAAAAAAAAAOAAAAAAAAAAAAAAAAAYPAuntzs/lk6C+6JkakdDhc5zXww5d6mPBdFK+c3FQAAAACVAvkAAAAAAAAAAAAAAAAALE0VmAhfAS7Juzrcn3ZJ8q6XTRMsAztEdYPxjUNGlN+AAAAAlQL5AAAAAAAAAAAAAAAAAC87zZTiuFN3kMO5HLufFatDo63xg/2ve3ivM4Jh4iM8wAAAAJUC+QAAAAAAQAAAACxNFZgIXwEuybs63J92SfKul00TLAM7RHWD8Y1DRpTfgAAAAoAAAAYbGlmZV91bml2ZXJzZV9ldmVyeXRoaW5nAAAAAQAAAAI0MgAAAAEAAAAAvO82U4rhTd5DDuRy7nxWrQ6Ot8YP9r3t4rzOCYeIjPMAAAAKAAAABmZlbnRvbgAAAAEAAAAHRkVOVE9OIQAAAAEAAAAAvO82U4rhTd5DDuRy7nxWrQ6Ot8YP9r3t4rzOCYeIjPMAAAAKAAAABmZlbnRvbgAAAAAAAAABAAAAAAGDwLp7c7P5ZOgvuiZGpHQ4XOc18MOXepjwXRSvnNxUAAAABQAAAAAAAAAAAAAAAQAAAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAALE0VmAhfAS7Juzrcn3ZJ8q6XTRMsAztEdYPxjUNGlN+AAAABgAAAAJBYXJkdmFyawAAAAAAAAAAAYPAuntzs/lk6C+6JkakdDhc5zXww5d6mPBdFK+c3FQAAAAABfXhAAAAAAEAAAAAsTRWYCF8BLsm7OtyfdknyrpdNEywDO0R1g/GNQ0aU34AAAAGAAAAAkJlYXZlcgAAAAAAAAAAAAABg8C6e3Oz+WToL7omRqR0OFznNfDDl3qY8F0Ur5zcVAAAAAAF9eEAAAAAAQAAAACxNFZgIXwEuybs63J92SfKul00TLAM7RHWD8Y1DRpTfgAAAAYAAAACQ2hpbmNoaWxsYQAAAAAAAAGDwLp7c7P5ZOgvuiZGpHQ4XOc18MOXepjwXRSvnNxUAAAAAAX14QAAAAABAAAAAAGDwLp7c7P5ZOgvuiZGpHQ4XOc18MOXepjwXRSvnNxUAAAABgAAAAJDaGluY2hpbGxhAAAAAAAAc3b96I5M1hzA+ylKF4az8dBh9fLxyldGX6qTIhG5RtYAAAAABfXhAAAAAAEAAAAAAYPAuntzs/lk6C+6JkakdDhc5zXww5d6mPBdFK+c3FQAAAAHAAAAALE0VmAhfAS7Juzrcn3ZJ8q6XTRMsAztEdYPxjUNGlN+AAAAAgAAAAxBYXJkdmFyawAAAAAAAAABAAAAAQAAAAABg8C6e3Oz+WToL7omRqR0OFznNfDDl3qY8F0Ur5zcVAAAAAcAAAAAsTRWYCF8BLsm7OtyfdknyrpdNEywDO0R1g/GNQ0aU34AAAACAAAADENoaW5jaGlsbGEAAAAAAAEAAAABAAAAAAGDwLp7c7P5ZOgvuiZGpHQ4XOc18MOXepjwXRSvnNxUAAAAAQAAAACxNFZgIXwEuybs63J92SfKul00TLAM7RHWD8Y1DRpTfgAAAAJBYXJkdmFyawAAAAAAAAAAAYPAuntzs/lk6C+6JkakdDhc5zXww5d6mPBdFK+c3FQAAAAAAAACKwAAAAAAAAAEh4iM8wAAAEANoixyHmIKbwiugkD4+f+HdUg5+tGSPJwZ4ThL11tG6Nv7UOp+r5wFy4YSTu2+Lg9QKjCu25B+Qu0a6u/SyNUIDRpTfgAAAEC+bZMRDPD/7t3hEpzXmkEMg2o+ygWhTc1GpHwOLrOJUoWbAlz8WdZHoxcFctd/I4OkuqBZTHKBG+3JdRkiOGIKr5zcVAAAAEAuYCwGqAElB2ZCAUc8WAOMibwK8z5twildx02TnDepEhY4JbCcfOlfZigumczMRzHEqNvAXPrQcfnQVBTxlssPEblG1gAAAEBTKRyeWpa9Mjahs3H6xkdZytzIomjeXT91RIIFuM8MsPullyMQWnS9BILq+0jrUu9LTkCjDvc5wY31EIi+n+AC
actual:  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


   */
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

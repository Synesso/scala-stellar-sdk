package stellar.scala.sdk

import stellar.scala.sdk.op.Operation
import stellar.scala.sdk.resp.SubmitTransactionResponse

case class Transaction(operation: Operation, additionalOperations: Operation*) {

  private val BaseFee = 100

  def sign(key: KeyPair): SignedTransaction = ???

  def fee: Int = BaseFee * (additionalOperations.size + 1)

}

case class SignedTransaction(operations: Seq[Operation]) {

  def submit(network: Network): SubmitTransactionResponse = ???

}

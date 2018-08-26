package stellar.sdk.op

import org.stellar.sdk.xdr.ManageDataOp
import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.MANAGE_DATA
import stellar.sdk.XDRPrimitives._
import stellar.sdk.{PublicKey, PublicKeyOps}

import scala.util.Try

sealed trait ManageDataOperation extends Operation {
  val name: String

  override def toOperationBody: OperationBody = {
    val op = new ManageDataOp
    op.setDataName(str64(name))
    val body = new OperationBody
    body.setDiscriminant(MANAGE_DATA)
    body.setManageDataOp(op)
    body
  }
}

/**
  * Deletes a Data Entry (name/value pair) for an account.
  *
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-data endpoint doc]]
  */
case class DeleteDataOperation(name: String, sourceAccount: Option[PublicKeyOps] = None) extends ManageDataOperation

/**
  * Creates or updates a Data Entry (name/value pair) for an account.
  *
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-data endpoint doc]]
  */
case class WriteDataOperation(name: String, value: String, sourceAccount: Option[PublicKeyOps] = None) extends ManageDataOperation {
  override def toOperationBody: OperationBody = {
    val body = super.toOperationBody
    body.getManageDataOp.setDataValue(dataValue(value.getBytes("UTF-8")))
    body
  }
}


object ManageDataOperation {
  def from(op: ManageDataOp, source: Option[PublicKey]): Try[ManageDataOperation] = Try {
    val name = op.getDataName.getString64
    Option(op.getDataValue) match {
      case Some(value) => WriteDataOperation(name, new String(value.getDataValue, "UTF-8"), source)
      case _ => DeleteDataOperation(name, source)
    }
  }

}

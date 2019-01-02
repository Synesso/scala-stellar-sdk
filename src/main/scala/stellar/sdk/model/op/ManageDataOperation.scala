package stellar.sdk.model.op

import cats.data.State
import stellar.sdk.PublicKeyOps
import stellar.sdk.model.xdr.{Decode, Encode}

sealed trait ManageDataOperation extends Operation {
  val name: String
}

/**
  * Deletes a Data Entry (name/value pair) for an account.
  *
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-data endpoint doc]]
  */
case class DeleteDataOperation(name: String, sourceAccount: Option[PublicKeyOps] = None) extends ManageDataOperation {
  override def encode: Stream[Byte] = super.encode ++ Encode.int(10) ++ Encode.string(name) ++ Encode.int(0)
}

/**
  * Creates or updates a Data Entry (name/value pair) for an account.
  *
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-data endpoint doc]]
  */
case class WriteDataOperation(name: String, value: String, sourceAccount: Option[PublicKeyOps] = None) extends ManageDataOperation {
  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(10) ++
      Encode.string(name) ++
      Encode.int(1) ++
      Encode.string(value)
}


object ManageDataOperation {
  def decode: State[Seq[Byte], ManageDataOperation] = for {
    name <- Decode.string
    value <- Decode.opt(Decode.string)
  } yield value match {
    case Some(v) => WriteDataOperation(name, v)
    case None => DeleteDataOperation(name)
  }

}

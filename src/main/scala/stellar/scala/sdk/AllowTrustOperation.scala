package stellar.scala.sdk

import org.stellar.sdk.xdr.AssetType._
import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType._
import org.stellar.sdk.xdr.{AccountID, AllowTrustOp}

import scala.util.Try

case class AllowTrustOperation(trustor: VerifyingKey,
                               assetCode: String,
                               authorize: Boolean,
                               sourceAccount: Option[KeyPair] = None) extends Operation with ByteArrays {

  override def toOperationBody: OperationBody = {
    val op = new AllowTrustOp()
    op.setTrustor(new AccountID())
    op.getTrustor.setAccountID(trustor.getXDRPublicKey)
    op.setAsset(new AllowTrustOp.AllowTrustOpAsset)
    if (assetCode.length <= 4) {
      op.getAsset.setAssetCode4(paddedByteArray(assetCode, 4))
      op.getAsset.setDiscriminant(ASSET_TYPE_CREDIT_ALPHANUM4)
    } else {
      op.getAsset.setAssetCode12(paddedByteArray(assetCode, 12))
      op.getAsset.setDiscriminant(ASSET_TYPE_CREDIT_ALPHANUM12)
    }
    op.setAuthorize(authorize)
    val body = new OperationBody
    body.setDiscriminant(ALLOW_TRUST)
    body.setAllowTrustOp(op)
    body
  }

}

object AllowTrustOperation {
  def from(op: AllowTrustOp): Try[AllowTrustOperation] = Try {
    AllowTrustOperation(
      trustor = KeyPair.fromXDRPublicKey(op.getTrustor.getAccountID),
      assetCode = (op.getAsset.getDiscriminant: @unchecked) match {
        case ASSET_TYPE_CREDIT_ALPHANUM4 => new String(op.getAsset.getAssetCode4).trim
        case ASSET_TYPE_CREDIT_ALPHANUM12 => new String(op.getAsset.getAssetCode12).trim
      },
      authorize = op.getAuthorize
    )
  }

  def apply(sourceAccount: KeyPair,
            trustor: VerifyingKey,
            assetCode: String,
            authorize: Boolean): AllowTrustOperation =
    AllowTrustOperation(trustor, assetCode, authorize, Some(sourceAccount))

}

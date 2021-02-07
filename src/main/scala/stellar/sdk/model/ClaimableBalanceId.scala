package stellar.sdk.model

import okio.ByteString
import org.stellar.xdr.{ClaimableBalanceID, ClaimableBalanceIDType, Hash}

sealed trait ClaimableBalanceId {
  def xdr: ClaimableBalanceID
  def encodeString: String
}

object ClaimableBalanceId {
  def decodeXdr(xdr: ClaimableBalanceID): ClaimableBalanceId = {
    xdr.getDiscriminant match {
      case ClaimableBalanceIDType.CLAIMABLE_BALANCE_ID_TYPE_V0 =>
        ClaimableBalanceHashId(new ByteString(xdr.getV0.getHash))
    }
  }

  def decode(xdr: ByteString): ClaimableBalanceId = decodeXdr(ClaimableBalanceID.decode(xdr))
}

case class ClaimableBalanceHashId(hash: ByteString) extends ClaimableBalanceId {
  def xdr: ClaimableBalanceID =
    new ClaimableBalanceID.Builder()
      .discriminant(ClaimableBalanceIDType.CLAIMABLE_BALANCE_ID_TYPE_V0)
      .v0(new Hash(hash.toByteArray))
      .build()

  override def encodeString: String = xdr.encode().hex()
}

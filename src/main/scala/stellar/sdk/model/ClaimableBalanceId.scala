package stellar.sdk.model

import okio.ByteString
import org.stellar.xdr.{ClaimableBalanceID, ClaimableBalanceIDType, Hash}

sealed trait ClaimableBalanceId {
  def encodeString: String = xdr.encode().hex()
  def xdr: ClaimableBalanceID
}

object ClaimableBalanceId {

  def decode(xdr: ClaimableBalanceID): ClaimableBalanceId = {
    xdr.getDiscriminant match {
      case ClaimableBalanceIDType.CLAIMABLE_BALANCE_ID_TYPE_V0 =>
        ClaimableBalanceHashId(new ByteString(xdr.getV0.getHash))
    }
  }

  def decode(bs: ByteString): ClaimableBalanceHashId = {
    val decoded = ClaimableBalanceID.decode(bs)
    decoded.getDiscriminant match {
      case ClaimableBalanceIDType.CLAIMABLE_BALANCE_ID_TYPE_V0 =>
        ClaimableBalanceHashId(decoded.getV0.encode())
    }
  }
}

case class ClaimableBalanceHashId(hash: ByteString) extends ClaimableBalanceId {
  def xdr: ClaimableBalanceID =
    new ClaimableBalanceID.Builder()
      .discriminant(ClaimableBalanceIDType.CLAIMABLE_BALANCE_ID_TYPE_V0)
      .v0(new Hash(hash.toByteArray))
      .build()
}

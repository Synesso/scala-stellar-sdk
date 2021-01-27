package stellar.sdk.model

import cats.data.State
import okio.ByteString
import org.stellar.xdr.{ClaimableBalanceID, ClaimableBalanceIDType, Hash}
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}

sealed trait ClaimableBalanceId extends Encodable {
  def xdr: ClaimableBalanceID
  def encodeString: String = xdr.encode().hex()
}

object ClaimableBalanceId extends Decode {
  def decodeXdr(xdr: ClaimableBalanceID): ClaimableBalanceId = {
    xdr.getDiscriminant match {
      case ClaimableBalanceIDType.CLAIMABLE_BALANCE_ID_TYPE_V0 =>
        ClaimableBalanceHashId(new ByteString(xdr.getV0.getHash))
    }
  }

  def decode: State[Seq[Byte], ClaimableBalanceId] = switch[ClaimableBalanceId](
    widen(bytes(32).map(_.toArray).map(new ByteString(_)).map(ClaimableBalanceHashId))
  )
}

case class ClaimableBalanceHashId(hash: ByteString) extends ClaimableBalanceId {
  override def encode: LazyList[Byte] = {
    val array = hash.toByteArray
    Encode.int(0) ++ Encode.bytes(32, array)
  }

  def xdr: ClaimableBalanceID =
    new ClaimableBalanceID.Builder()
      .discriminant(ClaimableBalanceIDType.CLAIMABLE_BALANCE_ID_TYPE_V0)
      .v0(new Hash(hash.toByteArray))
      .build()
}

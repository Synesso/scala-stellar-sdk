package stellar.sdk.op

import cats.data.State
import stellar.sdk.ByteArrays._
import stellar.sdk.xdr.{Decode, Encode}
import stellar.sdk.{KeyPair, _}

/**
  * Updates the “authorized” flag of an existing trust line. This is called by the issuer of the related asset.
  */
case class AllowTrustOperation(trustor: PublicKeyOps,
                               assetCode: String, // todo - make nonnativeasset
                               authorize: Boolean,
                               sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(7) ++
      trustor.encode ++
      (if (assetCode.length <= 4) Encode.int(1) ++ Encode.bytes(4, paddedByteArray(assetCode, 4))
      else Encode.int(2) ++ Encode.bytes(12, paddedByteArray(assetCode, 12))) ++
      Encode.bool(authorize)

}

object AllowTrustOperation {
  def decode: State[Seq[Byte], AllowTrustOperation] = for {
    trustor <- KeyPair.decode
    assetCodeLength <- Decode.int.map {
      case 1 => 4
      case 2 => 12
    }
    assetCode <- Decode.bytes(assetCodeLength).map(_.toArray).map(ByteArrays.paddedByteArrayToString)
    authorize <- Decode.bool
  } yield AllowTrustOperation(trustor, assetCode, authorize)
}

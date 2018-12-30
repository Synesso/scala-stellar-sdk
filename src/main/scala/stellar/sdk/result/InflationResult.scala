package stellar.sdk.result

import cats.data.State
import stellar.sdk.xdr.{Decode, Encodable, Encode}
import stellar.sdk.{KeyPair, NativeAmount, PublicKey}

sealed abstract class InflationResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 9)

object InflationResult {
  val decode: State[Seq[Byte], InflationResult] = Decode.int.flatMap {
    case 0 => Decode.arr(InflationPayout.decode).map(InflationSuccess)
    case -1 => State.pure(InflationNotDue)
  }
}

/**
  * Inflation operation was successful.
  */
case class InflationSuccess(payouts: Seq[InflationPayout]) extends InflationResult(0) {
  override def encode: Stream[Byte] = super.encode ++ Encode.arr(payouts)
}

/**
  * Inflation operation failed because inflation is not yet due.
  */
case object InflationNotDue extends InflationResult(-1)


case class InflationPayout(recipient: PublicKey, amount: NativeAmount) extends Encodable {
  def encode: Stream[Byte] = recipient.encode ++ Encode.long(amount.units)
}

object InflationPayout {
  // todo - serde spec
  val decode: State[Seq[Byte], InflationPayout] = for {
    recipient <- KeyPair.decode
    units <- Decode.long
  } yield InflationPayout(recipient, NativeAmount(units))
}
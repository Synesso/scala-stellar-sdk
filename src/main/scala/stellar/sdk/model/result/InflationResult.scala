package stellar.sdk.model.result

import cats.data.State
import stellar.sdk.model.NativeAmount
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.{KeyPair, PublicKey}

sealed abstract class InflationResult(val opResultCode: Int) extends ProcessedOperationResult(opCode = 9)

object InflationResult extends Decode {
  val decode: State[Seq[Byte], InflationResult] = int.flatMap {
    case 0 => arr(InflationPayout.decode).map(InflationSuccess)
    case -1 => State.pure(InflationNotDue)
  }
}

/**
  * Inflation operation was successful.
  */
case class InflationSuccess(payouts: Seq[InflationPayout]) extends InflationResult(0) {
  override def encode: LazyList[Byte] = super.encode ++ Encode.arr(payouts)
}

/**
  * Inflation operation failed because inflation is not yet due.
  */
case object InflationNotDue extends InflationResult(-1)


case class InflationPayout(recipient: PublicKey, amount: NativeAmount) extends Encodable {
  def encode: LazyList[Byte] = recipient.encode ++ Encode.long(amount.units)
}

object InflationPayout extends Decode {
  val decode: State[Seq[Byte], InflationPayout] = for {
    recipient <- KeyPair.decode
    units <- long
  } yield InflationPayout(recipient, NativeAmount(units))
}
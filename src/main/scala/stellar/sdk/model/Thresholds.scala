package stellar.sdk.model

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encodable}
import stellar.sdk.model.xdr.Encode._

case class Thresholds(low: Int, med: Int, high: Int) extends Encodable {
  override def encode: Stream[Byte] = bytes(4, Array[Byte](low.toByte, med.toByte, high.toByte, 0))
}

object Thresholds extends Decode {
  // TODO (jem) - Confirm that the bytes are mapped correctly.
  val decode: State[Seq[Byte], Thresholds] = bytes(4).map { bs =>
    val Seq(low, med, high, _): Seq[Int] = bs.map(_ & 0xff)
    Thresholds(low, med, high)
  }
}
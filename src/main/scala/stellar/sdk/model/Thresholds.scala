package stellar.sdk.model

import cats.data.State
import stellar.sdk.model.xdr.Decode

case class Thresholds(low: Int, med: Int, high: Int)

object Thresholds extends Decode {
  val decode: State[Seq[Byte], Thresholds] = bytes(4).map { bs =>
    val Seq(low, med, high, _): Seq[Int] = bs.map(_.toInt)
    Thresholds(low, med, high)
  }
}
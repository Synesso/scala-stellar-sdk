package stellar.sdk.model

import java.util.Locale

import cats.data.State
import stellar.sdk.model.xdr.{Decode, Encode}

case class Price(n: Int, d: Int) {
  def asDecimalString = "%.7f".formatLocal(Locale.ROOT, n * 1.0 / d * 1.0)

  def encode: Stream[Byte] = Encode.int(n) ++ Encode.int(d)
}

object Price {
  def decode: State[Seq[Byte], Price] = for {
    n <- Decode.int
    d <- Decode.int
  } yield Price(n, d)
}

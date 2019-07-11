package stellar.sdk.model.xdr

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}
import java.time.Instant

import cats.data.State

object Decode {

  val int: State[Seq[Byte], Int] = State[Seq[Byte], Int] {
    case bs if bs.length >= 4 =>
      bs.drop(4) -> ByteBuffer.wrap(bs.take(4).toArray).getInt
    case _ => throw new EOFException("Insufficient data remains to parse an int")
  }

  val long: State[Seq[Byte], Long] = State[Seq[Byte], Long] {
    case bs if bs.length >= 8 =>
      bs.drop(8) -> ByteBuffer.wrap(bs.take(8).toArray).getLong
    case _ => throw new EOFException("Insufficient data remains to parse a long")
  }

  val instant: State[Seq[Byte], Instant] = long.map(Instant.ofEpochSecond)

  val bool: State[Seq[Byte], Boolean] = int.map(_ == 1)

  def bytes(len: Int): State[Seq[Byte], Seq[Byte]] = State[Seq[Byte], Seq[Byte]] {
    case bs if bs.length >= len => bs.drop(len) -> bs.take(len)
    case _ => throw new EOFException(s"Insufficient data remains to parse $len bytes")
  }

  val bytes: State[Seq[Byte], Seq[Byte]] = for {
    len <- int
    bs <- bytes(len)
  } yield bs

  def padded(multipleOf: Int = 4): State[Seq[Byte], Seq[Byte]] = for {
    len <- int
    bs <- bytes(len)
    _ <- bytes((multipleOf - (len % multipleOf)) % multipleOf)
  } yield bs

  val string: State[Seq[Byte], String] = padded().map(_.toArray).map(new String(_, StandardCharsets.UTF_8))

  def opt[T](parseT: State[Seq[Byte], T]): State[Seq[Byte], Option[T]] = bool.flatMap {
    case true => parseT.map(Some(_))
    case false => State.pure(None)
  }

  def arr[T](parseT: State[Seq[Byte], T]): State[Seq[Byte], Seq[T]] = {
    def inner(qty: Int, ts: Seq[T]): State[Seq[Byte], Seq[T]] = qty match {
      case 0 => State.pure(ts)
      case _ => for {
        t <- parseT
        ts_ <- inner(qty - 1, t +: ts)
      } yield ts_
    }
    int.flatMap{i =>
      inner(i, Nil)
    }.map(_.reverse)
  }
}


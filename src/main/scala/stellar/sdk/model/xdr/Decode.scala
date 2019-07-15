package stellar.sdk.model.xdr

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Instant

import cats.data.State
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

trait Decode extends LazyLogging {

  private def decode[T](bs: Seq[Byte], len: Int)(decoder: Seq[Byte] => T): (Seq[Byte], T) = {
    if (bs.length < len) throw new EOFException("Insufficient data remains to parse.")
    val t = decoder(bs.take(len))
    logger.debug(s"Dropping {} to make {}", len, t)
    bs.drop(len) -> t
  }

  val int: State[Seq[Byte], Int] = State[Seq[Byte], Int] { bs =>
    decode(bs, 4) { in => ByteBuffer.wrap(in.toArray).getInt }
  }

  val long: State[Seq[Byte], Long] = State[Seq[Byte], Long] { bs =>
    decode(bs, 8) { in => ByteBuffer.wrap(in.toArray).getLong }
  }

  val instant: State[Seq[Byte], Instant] = long.map(Instant.ofEpochSecond)

  val bool: State[Seq[Byte], Boolean] = int.map(_ == 1)

  def bytes(len: Int): State[Seq[Byte], Seq[Byte]] = State[Seq[Byte], Seq[Byte]] { bs =>
    decode(bs, len) { _.take(len) }
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

  // TODO (jem) - Use switch everywhere that we currently do this manually.
  def switch[T](zero: State[Seq[Byte], T], others: State[Seq[Byte], T]*) = int.flatMap {
    case 0 => zero
    case n =>  Try(others(n - 1)).getOrElse {
      throw new IllegalArgumentException(s"No parser defined for discriminant $n")
    }
  }

  def opt[T](parseT: State[Seq[Byte], T]): State[Seq[Byte], Option[T]] = bool.flatMap {
    case true => parseT.map(Some(_))
    case false => State.pure(None)
  }

  def arr[T](parseT: State[Seq[Byte], T]): State[Seq[Byte], Seq[T]] = int.flatMap(seq(_, parseT))

  def seq[T](qty: Int, parseT: State[Seq[Byte], T]): State[Seq[Byte], Seq[T]] = {
    def inner(remain: Int, ts: Seq[T]): State[Seq[Byte], Seq[T]] = remain match {
      case 0 => State.pure(ts)
      case _ => for {
        t <- parseT
        ts_ <- inner(remain - 1, t +: ts)
      } yield ts_
    }
    inner(qty, Nil).map(_.reverse)
  }

  def drop[T](parse: State[Seq[Byte], _])(t: T): State[Seq[Byte], T] = for {
    _ <- parse
  } yield t

  def widen[A, W, O <: W](s: State[A, O]): State[A, W] = s.map(w => w: W)
}


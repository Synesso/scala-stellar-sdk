package stellar.sdk.model.xdr

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Instant

import cats.data.State
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

trait Decode extends LazyLogging {

  def log[T](t: T): State[Seq[Byte], Unit] = State[Seq[Byte], Unit] { bs =>
    logger.debug("{}\n", t)
    bs -> Unit
  }

  private def decode[T](bs: Seq[Byte], len: Int)(decoder: Seq[Byte] => T): (Seq[Byte], T) = {
    if (bs.length < len) throw new EOFException("Insufficient data remains to parse.")
    val t = decoder(bs.take(len))
    logger.trace(s"Dropping {} to make {}", len, t)
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

  // TODO (jem) - Improve as per notes from Ben.
  /*
  benhutchison 8:35 PM
@jem this is a simpler way to implement your seq (except it wont work for Seq, more on that below):
Welcome to the Ammonite Repl 1.6.9
(Scala 2.13.0 Java 11.0.1)
If you like Ammonite, please support our development at www.patreon.com/lihaoyi
@ import $ivy.`org.typelevel::cats-core:2.0.0-M4`
import $ivy.$

@ import cats.data._
import cats.data._

@ import cats.implicits._
import cats.implicits._

@ object Test {
    def list[T](qty: Int, parseT: State[List[Byte], T]): State[List[Byte], List[T]] = List.range(0, qty).foldMapM(_ => parseT.map(List(_)))
  }
defined object Test
Lets just translate to english some of the bits:
List.range(0, qty) just so we can do something qty times
foldMapM(_ => parseT.map(List(_))) each time, run parseT and put the result into a List, then it auto-concats all the lists together using Monoid
we actually throw out the items from the list, its just for side effect, hence the _ =>
Now, some gotchas:
- Type inference, hence I using  2.13.0, thats not accident. State monads needs -Ypartial-unification in 2.12 or else 2.13 has it on by default
- No more Seq. The functional world uses List or Vector, you got to embrace that if you want to play State monads with the big boys. Typeclass abstraction and object-oriented abstraction are uneasy bedfellows.
benhutchison 8:59 PM
See also eg https://github.com/typelevel/cats/issues/277

GitHubGitHub
What's policy around typeclass instances for std lib collection classes? · Issue #277 · typelevel/cats
More a question, but seems too involved to ask on Gitter: what&#39;s the policy & thinking around what stdlib collections have typeclass instances provided? (ie in cats.std) In scalaz, instance...
jem 9:48 PM
That's awesome. Thanks @benhutchison
benhutchison 9:50 PM
Turns out you can do better. Its kinda an obsessive thing for me :upside_down_face:
@ import cats._
import cats._

@ object Test {
    def list[T](qty: Int, parseT: State[List[Byte], T]): State[List[Byte], List[T]] = parseT.replicateA(qty)
  }
defined object Test
WTF? replicateA(qty) means, do the thing (parseT) qty times, inside an applicative effect (hence the A after replicate), which in your case is  State[List[Byte], ?], and then return the results in a List
...and of course, the whole return is wrapped inside the applicative effect type, hence State[List[Byte], List[T]]
this story ties into the discovery, just in 2007-2009, that lots of things we thought Monads were needed for actually needed this weaker abstraction Applicative
What makes your problem structure "Applicative" rather than "Monadic" is that each parseT is independent of the others,
ie you can interpret some bytes without knowing what bytes came before.. (edited)


   */
  def seq[T](qty: Int, parseT: State[Seq[Byte], T]): State[Seq[Byte], Seq[T]] = {
    (0 until qty).foldLeft(State.pure[Seq[Byte], Seq[T]](Seq.empty[T])) { case (state, _) =>
      for {
        ts <- state
        t <- parseT
      } yield ts :+ t
    }
  }

  def drop[T](parse: State[Seq[Byte], _])(t: T): State[Seq[Byte], T] = for {
    _ <- parse
  } yield t

  def widen[A, W, O <: W](s: State[A, O]): State[A, W] = s.map(w => w: W)
}

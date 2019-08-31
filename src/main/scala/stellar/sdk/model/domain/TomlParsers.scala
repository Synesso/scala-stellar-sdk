package stellar.sdk.model.domain

import akka.http.scaladsl.model.Uri
import stellar.sdk.{KeyPair, PublicKey}
import toml.Value
import toml.Value.{Arr, Bool, Num, Str, Tbl}

trait TomlParsers {
  val string: PartialFunction[Value, String] = { case Str(s) => s }
  val bool: PartialFunction[Value, Boolean] = { case Bool(b) => b }
  val long: PartialFunction[Value, Long] = { case Num(l) => l }
  val int: PartialFunction[Value, Int] = long.andThen(_.toInt)
  val uri: PartialFunction[Value, Uri] = { case Str(s) => Uri(s) }
  val publicKey: PartialFunction[Value, PublicKey] = { case Str(s) => KeyPair.fromAccountId(s) }
  def array[T](inner: PartialFunction[Value, T]): PartialFunction[Value, List[T]] = {
    case Arr(values) => values.map(inner)
  }
  def parseTomlValue[T](tbl: Tbl, key: String, parser: PartialFunction[Value, T]): Option[T] =
    tbl.values.get(key).map(parser.applyOrElse(_, {
      v: Value => throw DomainInfoParseException(s"value for $key was not of the expected type. [value=$v]")
    }))

}

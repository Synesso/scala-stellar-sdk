package stellar.sdk.model

import cats.data.State
import org.json4s.{DefaultFormats, Formats}
import org.json4s.JsonAST.JValue
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays._
import stellar.sdk.{KeyPair, PublicKeyOps}

sealed trait Asset extends Encodable {
  val code: String
  def canoncialString: String
}

object Asset extends Decode {
  implicit val formats: Formats = DefaultFormats

  def apply(code: String, issuer: PublicKeyOps): NonNativeAsset = {
    require(code.matches("[a-zA-Z0-9?]+"), s"Asset code $code does not match [a-zA-Z0-9]+")
    if (code.length <= 4) IssuedAsset4.of(code, issuer) else IssuedAsset12.of(code, issuer)
  }

  val decode: State[Seq[Byte], Asset] = int.flatMap {
    case 0 => State.pure(NativeAsset)
    case 1 => IssuedAsset4.decode.map(x => x: Asset)
    case 2 => IssuedAsset12.decode.map(x => x: Asset)
  }

  def parseAsset(prefix: String = "", obj: JValue) = {

    def assetCode = (obj \ s"${prefix}asset_code").extract[String]

    def assetIssuer = KeyPair.fromAccountId((obj \ s"${prefix}asset_issuer").extract[String])

    (obj \ "asset").extractOpt[String].map(_.split(":")) match {
      case Some(Array(code, issuer)) if code.length > 4 => IssuedAsset12(code, KeyPair.fromAccountId(issuer))
      case Some(Array(code, issuer)) => IssuedAsset4(code, KeyPair.fromAccountId(issuer))
      case Some(Array("native")) => NativeAsset
      case _ =>
        (obj \ s"${prefix}asset_type").extract[String] match {
          case "native" => NativeAsset
          case "credit_alphanum4" => IssuedAsset4(assetCode, assetIssuer)
          case "credit_alphanum12" => IssuedAsset12(assetCode, assetIssuer)
          case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
        }
    }
  }
}

case object NativeAsset extends Asset {
  val code: String = "XLM"
  override def encode: LazyList[Byte] = Encode.int(0)
  def canoncialString: String = "native"
}

sealed trait NonNativeAsset extends Asset {
  val code: String
  val issuer: PublicKeyOps
  val typeString: String

  def canoncialString: String = s"$code:${issuer.accountId}"
  override def toString: String = canoncialString
}

/**
  * Represents all assets with codes 1-4 characters long.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/assets.html" target="_blank">Assets</a>
  */
case class IssuedAsset4 private(code: String, issuer: PublicKeyOps) extends NonNativeAsset {
  assert(code.nonEmpty, s"Asset's code '$code' cannot be empty")
  assert(code.length <= 4, s"Asset's code '$code' should have length no greater than 4")

  override val typeString = "credit_alphanum4"

  def encode: LazyList[Byte] = {
    val codeBytes = paddedByteArray(code, 4)
    Encode.int(1) ++ Encode.bytes(4, codeBytes) ++ issuer.encode
  }
}

object IssuedAsset4 extends Decode {
  def of(code: String, issuer: PublicKeyOps): IssuedAsset4 = IssuedAsset4(code, issuer.asPublicKey)

  def decode: State[Seq[Byte], IssuedAsset4] = for {
    bs <- bytes(4)
    issuer <- KeyPair.decode
    code = paddedByteArrayToString(bs.toArray)
  } yield IssuedAsset4(code, issuer)
}


/**
  * Represents all assets with codes 5-12 characters long.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/assets.html" target="_blank">Assets</a>
  */
case class IssuedAsset12 private (code: String, issuer: PublicKeyOps) extends NonNativeAsset {
  assert(code.length >= 5 && code.length <= 12, s"Asset's code '$code' should have length between 5 & 12 inclusive")

  override val typeString = "credit_alphanum12"

  def encode: LazyList[Byte] = {
    val codeBytes = paddedByteArray(code, 12)
    Encode.int(2) ++ Encode.bytes(12, codeBytes) ++ issuer.encode
  }
}

object IssuedAsset12 extends Decode {
  def of(code: String, keyPair: PublicKeyOps): IssuedAsset12 = IssuedAsset12(code, keyPair.asPublicKey)

  def decode: State[Seq[Byte], IssuedAsset12] = for {
    bs <- bytes(12)
    issuer <- KeyPair.decode
    code = paddedByteArrayToString(bs.toArray)
  } yield IssuedAsset12(code, issuer)
}

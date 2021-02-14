package stellar.sdk.model

import okio.ByteString
import org.json4s.JsonAST.JValue
import org.json4s.{DefaultFormats, Formats}
import org.stellar.xdr.Asset.{AssetAlphaNum12, AssetAlphaNum4}
import org.stellar.xdr.{AllowTrustOp, AssetCode12, AssetCode4, AssetType, Asset => XAsset}
import stellar.sdk.util.ByteArrays._
import stellar.sdk.{KeyPair, PublicKeyOps}

sealed trait Asset {
  val code: String
  def canoncialString: String
  def xdr: XAsset
  def encode: ByteString = xdr.encode()
}

object Asset {
  def decodeCode(asset: AllowTrustOp.AllowTrustOpAsset): String = {
    asset.getDiscriminant match {
      case AssetType.ASSET_TYPE_CREDIT_ALPHANUM4 =>
        paddedByteArrayToString(asset.getAssetCode4.getAssetCode4)
      case AssetType.ASSET_TYPE_CREDIT_ALPHANUM12 =>
        paddedByteArrayToString(asset.getAssetCode12.getAssetCode12)
      case AssetType.ASSET_TYPE_NATIVE => "XLM"
    }
  }

  def decodeXdr(asset: XAsset): Asset = {
    asset.getDiscriminant match {
      case AssetType.ASSET_TYPE_NATIVE => NativeAsset
      case AssetType.ASSET_TYPE_CREDIT_ALPHANUM4 =>
        IssuedAsset4(
          code = paddedByteArrayToString(asset.getAlphaNum4.getAssetCode.getAssetCode4),
          issuer = AccountId.decodeXdr(asset.getAlphaNum4.getIssuer).publicKey
        )
      case AssetType.ASSET_TYPE_CREDIT_ALPHANUM12 =>
        IssuedAsset12(
          code = paddedByteArrayToString(asset.getAlphaNum12.getAssetCode.getAssetCode12),
          issuer = AccountId.decodeXdr(asset.getAlphaNum12.getIssuer).publicKey
        )
    }
  }

  implicit val formats: Formats = DefaultFormats

  def apply(code: String, issuer: PublicKeyOps): NonNativeAsset = {
    require(code.matches("[a-zA-Z0-9?]+"), s"Asset code $code does not match [a-zA-Z0-9]+")
    if (code.length <= 4) IssuedAsset4.of(code, issuer) else IssuedAsset12.of(code, issuer)
  }

  private val IssuedAssetRegex = "([a-zA-Z0-9]{1,12}):([A-Z0-9]{56})".r
  def parseIssuedAsset(code: String): NonNativeAsset =
    code match {
      case IssuedAssetRegex(code, issuer) => apply(code, KeyPair.fromAccountId(issuer))
      case _ => throw AssetException(s"Cannot parse issued asset [code=$code]")
    }

  def parseAsset(prefix: String = "", obj: JValue): Asset = {

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
          case t => throw AssetException(s"Unrecognised asset type '$t'")
        }
    }
  }
}

case object NativeAsset extends Asset {
  val code: String = "XLM"
  def canoncialString: String = "native"
  override def xdr: XAsset = new XAsset.Builder().discriminant(AssetType.ASSET_TYPE_NATIVE).build()
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

  override def xdr: XAsset = new XAsset.Builder()
    .discriminant(AssetType.ASSET_TYPE_CREDIT_ALPHANUM4)
    .alphaNum4(new AssetAlphaNum4.Builder()
      .assetCode(new AssetCode4(paddedByteArray(code, 4)))
      .issuer(issuer.toAccountId.xdr)
      .build())
    .build()
}

object IssuedAsset4 {
  def of(code: String, issuer: PublicKeyOps): IssuedAsset4 = IssuedAsset4(code, issuer.asPublicKey)
}


/**
  * Represents all assets with codes 5-12 characters long.
  *
  * @see <a href="https://www.stellar.org/developers/learn/concepts/assets.html" target="_blank">Assets</a>
  */
case class IssuedAsset12 private (code: String, issuer: PublicKeyOps) extends NonNativeAsset {
  assert(code.length >= 5 && code.length <= 12, s"Asset's code '$code' should have length between 5 & 12 inclusive")

  override val typeString = "credit_alphanum12"

  override def xdr: XAsset = new XAsset.Builder()
    .discriminant(AssetType.ASSET_TYPE_CREDIT_ALPHANUM12)
    .alphaNum12(new AssetAlphaNum12.Builder()
      .assetCode(new AssetCode12(paddedByteArray(code, 12)))
      .issuer(issuer.toAccountId.xdr)
      .build())
    .build()
}

object IssuedAsset12 {
  def of(code: String, keyPair: PublicKeyOps): IssuedAsset12 = IssuedAsset12(code, keyPair.asPublicKey)
}

case class AssetException(msg: String) extends RuntimeException(msg)


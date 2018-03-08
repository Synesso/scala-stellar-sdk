package stellar.sdk.op

import org.apache.commons.codec.binary.Base64
import org.json4s.JsonAST.{JArray, JObject, JValue}
import org.json4s.{CustomSerializer, DefaultFormats}
import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType._
import org.stellar.sdk.xdr.{Operation => XDROp}
import stellar.sdk._

import scala.util.{Success, Try}

trait Operation {
  val sourceAccount: Option[PublicKeyOps]

  def toOperationBody: OperationBody

  def toXDR: XDROp = {
    val op = new org.stellar.sdk.xdr.Operation()
    //    val src = new AccountID()
    //    src.setAccountID(sourceAccount.getXDRPublicKey)
    //    op.setSourceAccount(src)
    op.setBody(toOperationBody)
    op
  }
}

object Operation {

  val ONE = BigDecimal(10).pow(7)

  def fromXDR(op: XDROp): Try[Operation] = {
    op.getBody.getDiscriminant match {
      case ALLOW_TRUST => AllowTrustOperation.from(op.getBody.getAllowTrustOp)
      case CHANGE_TRUST => ChangeTrustOperation.from(op.getBody.getChangeTrustOp)
      case CREATE_ACCOUNT => CreateAccountOperation.from(op.getBody.getCreateAccountOp)
      case PATH_PAYMENT => PathPaymentOperation.from(op.getBody.getPathPaymentOp)
      case PAYMENT => PaymentOperation.from(op.getBody.getPaymentOp)
      case SET_OPTIONS => SetOptionsOperation.from(op.getBody.getSetOptionsOp)
      case MANAGE_OFFER => ManageOfferOperation.from(op.getBody.getManageOfferOp)
      case CREATE_PASSIVE_OFFER => CreatePassiveOfferOperation.from(op.getBody.getCreatePassiveOfferOp)
      case ACCOUNT_MERGE => AccountMergeOperation.from(op.getBody)
      case INFLATION => Success(InflationOperation())
      case MANAGE_DATA => ManageDataOperation.from(op.getBody.getManageDataOp)
    }
  }
}

object OperationDeserializer extends CustomSerializer[Operation](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats

    def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])

    def asset(prefix: String = "", obj: JValue = o) = {
      def assetCode = (obj \ s"${prefix}asset_code").extract[String]

      def assetIssuer = KeyPair.fromAccountId((obj \ s"${prefix}asset_issuer").extract[String])

      (obj \ s"${prefix}asset_type").extract[String] match {
        case "native" => NativeAsset
        case "credit_alphanum4" => IssuedAsset4(assetCode, assetIssuer)
        case "credit_alphanum12" => IssuedAsset12(assetCode, assetIssuer)
        case t => throw new RuntimeException(s"Unrecognised asset type '$t'")
      }
    }

    def nonNativeAsset = asset().asInstanceOf[NonNativeAsset]

    def price(label: String = "price_r"): Price = Price(
      n = (o \ label \ "n").extract[Int],
      d = (o \ label \ "d").extract[Int]
    )

    def doubleFromString(key: String) = (o \ key).extract[String].toDouble

    def nativeAmount(key: String) = {
      NativeAmount(Amount.toBaseUnits(doubleFromString(key)).get)
    }

    def issuedAmount(label: String) = amount(label).asInstanceOf[IssuedAmount]

    def amount(label: String = "amount", assetPrefix: String = "") = {
      val units = Amount.toBaseUnits(doubleFromString(label)).get
      asset(assetPrefix) match {
        case nna: NonNativeAsset => IssuedAmount(units, nna)
        case NativeAsset => NativeAmount(units)
      }
    }

    (o \ "type").extract[String] match {
      case "create_account" => CreateAccountOperation(account(), nativeAmount("starting_balance"))
      case "payment" => PaymentOperation(account("to"), amount())
      case "path_payment" =>
        val JArray(pathJs) = o \ "path"
        val path: List[Asset] = pathJs.map(a => asset(obj = a))
        PathPaymentOperation(amount("source_max", "source_"), account("to"), amount(), path)
      case "manage_offer" =>
        (o \ "offer_id").extract[Long] match {
          case 0L => CreateOfferOperation(
            selling = amount(assetPrefix = "selling_"),
            buying = asset("buying_"),
            price = price()
          )
          case id =>
            val amnt = (o \ "amount").extract[String].toDouble
            if (amnt == 0.0) {
              DeleteOfferOperation(id, asset("selling_"), asset("buying_"), price())
            } else {
              UpdateOfferOperation(id, selling = amount(assetPrefix = "selling_"), buying = asset("buying_"), price = price())
            }
        }
      case "create_passive_offer" =>
        CreatePassiveOfferOperation(
          selling = amount(assetPrefix = "selling_"),
          buying = asset("buying_"),
          price = price()
        )
      case "set_options" =>
        SetOptionsOperation(
          inflationDestination = (o \ "inflation_dest").extractOpt[String].map(KeyPair.fromAccountId),
          clearFlags = (o \ "clear_flags").extractOpt[Set[Int]].map(_.flatMap(IssuerFlags.apply)).filter(_.nonEmpty),
          setFlags = (o \ "set_flags").extractOpt[Set[Int]].map(_.flatMap(IssuerFlags.apply)).filter(_.nonEmpty),
          masterKeyWeight = (o \ "master_key_weight").extractOpt[Int],
          lowThreshold = (o \ "low_threshold").extractOpt[Int],
          mediumThreshold = (o \ "med_threshold").extractOpt[Int],
          highThreshold = (o \ "high_threshold").extractOpt[Int],
          homeDomain = (o \ "home_domain").extractOpt[String],
          signer = for {
            key <- (o \ "signer_key").extractOpt[String]
            weight <- (o \ "signer_weight").extractOpt[Int]
          } yield {
            AccountSigner(KeyPair.fromAccountId(key), weight)
          }
        )
      case "change_trust" =>
        ChangeTrustOperation(issuedAmount("limit"))
      case "allow_trust" =>
        val asset: NonNativeAsset = nonNativeAsset
        AllowTrustOperation(account("trustor"), asset.code, (o \ "authorize").extract[Boolean])
      case "account_merge" =>
        AccountMergeOperation(KeyPair.fromAccountId((o \ "into").extract[String]))
      case "inflation" =>
        InflationOperation()
      case "manage_data" =>
        val name = (o \ "name").extract[String]
        val value = (o \ "value").extract[String]
        value match {
          case "" => DeleteDataOperation(name)
          case _ => WriteDataOperation(name, new String(Base64.decodeBase64(value), "UTF-8"))
        }
      case t =>
        throw new RuntimeException(s"Unrecognised operation type '$t'")
    }
}, PartialFunction.empty)
)

/**
  * Marker trait for any operation that involves a payment (`PaymentOperation`, `CreateAccountOperation`)
  */
trait PayOperation extends Operation


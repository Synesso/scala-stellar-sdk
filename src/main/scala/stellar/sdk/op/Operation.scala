package stellar.sdk.op

import org.apache.commons.codec.binary.Base64
import org.json4s.JsonAST.{JArray, JObject, JValue}
import org.json4s.{CustomSerializer, DefaultFormats}
import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType._
import org.stellar.sdk.xdr.{AccountID, Operation => XDROp}
import stellar.sdk._

import scala.util.{Success, Try}

/**
  * An Operation represents a change to the ledger. It is the action, as opposed to the effects resulting from that action.
  */
trait Operation {
  val sourceAccount: Option[PublicKeyOps]

  def toOperationBody: OperationBody

  def toXDR: XDROp = {
    val op = new org.stellar.sdk.xdr.Operation()
    sourceAccount.foreach { source =>
      op.setSourceAccount(new AccountID)
      op.getSourceAccount.setAccountID(source.getXDRPublicKey)
    }
    op.setBody(toOperationBody)
    op
  }
}

object Operation {

  val ONE = BigDecimal(10).pow(7)

  // todo - these should not be Trys because any received xdr object should be correct
  def fromXDR(op: XDROp): Try[Operation] = {
    val source = Option(op.getSourceAccount).map(_.getAccountID).map(KeyPair.fromXDRPublicKey)
    op.getBody.getDiscriminant match {
      case ALLOW_TRUST => AllowTrustOperation.from(op.getBody.getAllowTrustOp, source)
      case CHANGE_TRUST => ChangeTrustOperation.from(op.getBody.getChangeTrustOp, source)
      case CREATE_ACCOUNT => CreateAccountOperation.from(op.getBody.getCreateAccountOp, source)
      case PATH_PAYMENT => PathPaymentOperation.from(op.getBody.getPathPaymentOp, source)
      case PAYMENT => PaymentOperation.from(op.getBody.getPaymentOp, source)
      case SET_OPTIONS => SetOptionsOperation.from(op.getBody.getSetOptionsOp, source)
      case MANAGE_OFFER => ManageOfferOperation.from(op.getBody.getManageOfferOp, source)
      case CREATE_PASSIVE_OFFER => CreatePassiveOfferOperation.from(op.getBody.getCreatePassiveOfferOp, source)
      case ACCOUNT_MERGE => AccountMergeOperation.from(op.getBody, source)
      case INFLATION => Success(InflationOperation(source))
      case MANAGE_DATA => ManageDataOperation.from(op.getBody.getManageDataOp, source)
      case BUMP_SEQUENCE => BumpSequenceOperation.from(op.getBody.getBumpSequenceOp, source)
    }
  }
}

object OperationDeserializer extends CustomSerializer[Operation](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats

    def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])

    def sourceAccount: Option[PublicKey] = Some(account("source_account"))

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
      case "create_account" => CreateAccountOperation(account(), nativeAmount("starting_balance"), sourceAccount)
      case "payment" => PaymentOperation(account("to"), amount(), sourceAccount)
      case "path_payment" =>
        val JArray(pathJs) = o \ "path"
        val path: List[Asset] = pathJs.map(a => asset(obj = a))
        PathPaymentOperation(amount("source_max", "source_"), account("to"), amount(), path, sourceAccount)
      case "manage_offer" =>
        (o \ "offer_id").extract[Long] match {
          case 0L => CreateOfferOperation(
            selling = amount(assetPrefix = "selling_"),
            buying = asset("buying_"),
            price = price(),
            sourceAccount = sourceAccount
          )
          case id =>
            val amnt = (o \ "amount").extract[String].toDouble
            if (amnt == 0.0) {
              DeleteOfferOperation(id, asset("selling_"), asset("buying_"), price(), sourceAccount)
            } else {
              UpdateOfferOperation(id, selling = amount(assetPrefix = "selling_"), buying = asset("buying_"),
                price = price(), sourceAccount)
            }
        }
      case "create_passive_offer" =>
        CreatePassiveOfferOperation(
          selling = amount(assetPrefix = "selling_"),
          buying = asset("buying_"),
          price = price(),
          sourceAccount = sourceAccount
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
          },
          sourceAccount = sourceAccount
        )
      case "change_trust" =>
        ChangeTrustOperation(issuedAmount("limit"), sourceAccount)
      case "allow_trust" =>
        val asset: NonNativeAsset = nonNativeAsset
        AllowTrustOperation(account("trustor"), asset.code, (o \ "authorize").extract[Boolean], sourceAccount)
      case "account_merge" =>
        AccountMergeOperation(KeyPair.fromAccountId((o \ "into").extract[String]), sourceAccount)
      case "inflation" =>
        InflationOperation(sourceAccount)
      case "manage_data" =>
        val name = (o \ "name").extract[String]
        val value = (o \ "value").extract[String]
        value match {
          case "" => DeleteDataOperation(name, sourceAccount)
          case _ => WriteDataOperation(name, new String(Base64.decodeBase64(value), "UTF-8"), sourceAccount)
        }
      case "bump_sequence" =>
        BumpSequenceOperation((o \ "bump_to").extract[Long], sourceAccount)
      case t =>
        throw new RuntimeException(s"Unrecognised operation type '$t'")
    }
}, PartialFunction.empty)
)

/**
  * Marker trait for any operation that involves a payment (`PaymentOperation`, `CreateAccountOperation`)
  */
trait PayOperation extends Operation


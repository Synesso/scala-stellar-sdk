package stellar.sdk.model.op

import cats.data.State
import org.apache.commons.codec.binary.Base64
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JArray, JObject, JValue}
import stellar.sdk._
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}

/**
  * An Operation represents a change to the ledger. It is the action, as opposed to the effects resulting from that action.
  */
trait Operation extends Encodable {
  val sourceAccount: Option[PublicKeyOps]
  override def encode: Stream[Byte] = Encode.opt(sourceAccount)
}

object Operation {

  val decode: State[Seq[Byte], Operation] =
    Decode.opt(KeyPair.decode).flatMap { source =>
      Decode.int.flatMap {
        case 0 => widen(CreateAccountOperation.decode.map(_.copy(sourceAccount = source)))
        case 1 => widen(PaymentOperation.decode.map(_.copy(sourceAccount = source)))
        case 2 => widen(PathPaymentOperation.decode.map(_.copy(sourceAccount = source)))
        case 3 => widen(ManageOfferOperation.decode.map {
          case x: CreateOfferOperation => x.copy(sourceAccount = source)
          case x: UpdateOfferOperation => x.copy(sourceAccount = source)
          case x: DeleteOfferOperation => x.copy(sourceAccount = source)
        })
        case 4 => widen(CreatePassiveOfferOperation.decode.map(_.copy(sourceAccount = source)))
        case 5 => widen(SetOptionsOperation.decode.map(_.copy(sourceAccount = source)))
        case 6 => widen(ChangeTrustOperation.decode.map(_.copy(sourceAccount = source)))
        case 7 => widen(AllowTrustOperation.decode.map(_.copy(sourceAccount = source)))
        case 8 => widen(AccountMergeOperation.decode.map(_.copy(sourceAccount = source)))
        case 9 => State.pure(InflationOperation(sourceAccount = source))
        case 10 => widen(ManageDataOperation.decode.map {
          case x: DeleteDataOperation => x.copy(sourceAccount = source)
          case x: WriteDataOperation => x.copy(sourceAccount = source)
        })
        case 11 => widen(BumpSequenceOperation.decode.map(_.copy(sourceAccount = source)))
      }
    }
  private def widen[A, W, O <: W](s: State[A, O]): State[A, W] = s.map(w => w: W)

  def decodeXDR(base64: String): Operation = decode.run(ByteArrays.base64(base64)).value._2
}

object OperationDeserializer extends ResponseParser[Operation]({ o: JObject =>
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
      BumpSequenceOperation((o \ "bump_to").extract[String].toLong, sourceAccount)
    case t =>
      throw new RuntimeException(s"Unrecognised operation type '$t'")
  }
})

/**
  * Marker trait for any operation that involves a payment (`CreateAccountOperation`, `PaymentOperation`, `PathPaymentOperation`, `AccountMergeOperation`)
  */
trait PayOperation extends Operation


package stellar.sdk.model.op

import cats.data.State
import org.apache.commons.codec.binary.Base64
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JArray, JObject, JValue}
import stellar.sdk._
import stellar.sdk.model._
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.model.xdr.{Decode, Encodable, Encode}
import stellar.sdk.util.ByteArrays
import stellar.sdk.util.ByteArrays.paddedByteArray

/**
  * An Operation represents a change to the ledger. It is the action, as opposed to the effects resulting from that action.
  */
sealed trait Operation extends Encodable {
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
sealed trait PayOperation extends Operation

/**
  * Funds and creates a new account.
  *
  * @param destinationAccount the account to be created
  * @param startingBalance the amount of funds to send to it
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#create-account endpoint doc]]
  */
case class CreateAccountOperation(destinationAccount: PublicKeyOps,
                                  startingBalance: NativeAmount = Amount.lumens(1),
                                  sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(0) ++
      destinationAccount.encode ++
      Encode.long(startingBalance.units)
}

object CreateAccountOperation {
  val decode: State[Seq[Byte], CreateAccountOperation] = for {
    destination <- KeyPair.decode
    startingBalance <- Decode.long
  } yield CreateAccountOperation(destination, NativeAmount(startingBalance))
}


/**
  * Represents a payment from one account to another. This payment can be either a simple native asset payment or a
  * fiat asset payment.
  *
  * @param destinationAccount the recipient of the payment
  * @param amount the amount to be paid
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#payment endpoint doc]]
  */
case class PaymentOperation(destinationAccount: PublicKeyOps,
                            amount: Amount,
                            sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(1) ++
      destinationAccount.encode ++
      amount.encode

}

object PaymentOperation {

  def decode: State[Seq[Byte], PaymentOperation] = for {
    destination <- KeyPair.decode
    amount <- Amount.decode
  } yield PaymentOperation(destination, amount)

}

/**
  * Represents a payment from one account to another through a path. This type of payment starts as one type of asset
  * and ends as another type of asset. There can be other assets that are traded into and out of along the path.
  * Suitable orders must exist on the relevant order books for this operation to be successful.
  *
  * @param sendMax the maximum amount willing to be spent to effect the payment
  * @param destinationAccount the payment recipient
  * @param destinationAmount the exact amount to be received
  * @param path the intermediate assets to traverse (may be empty)
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#path-payment endpoint doc]]
  */
case class PathPaymentOperation(sendMax: Amount,
                                destinationAccount: PublicKeyOps,
                                destinationAmount: Amount,
                                path: Seq[Asset] = Nil,
                                sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(2) ++
      sendMax.encode ++
      destinationAccount.encode ++
      destinationAmount.encode ++
      Encode.arr(path)
}

object PathPaymentOperation {

  def decode: State[Seq[Byte], PathPaymentOperation] = for {
    sendMax <- Amount.decode
    destAccount <- KeyPair.decode
    destAmount <- Amount.decode
    path <- Decode.arr(Asset.decode)
  } yield PathPaymentOperation(sendMax, destAccount, destAmount, path)

}

sealed trait ManageOfferOperation extends Operation {
  val offerId: Long = 0
}

/**
  * Creates an offer in the Stellar network.
  */
case class CreateOfferOperation(selling: Amount, buying: Asset, price: Price,
                                sourceAccount: Option[PublicKeyOps] = None) extends ManageOfferOperation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(3) ++
      selling.asset.encode ++
      buying.encode ++
      Encode.long(selling.units) ++
      price.encode ++
      Encode.long(0)
}

/**
  * Deletes an offer in the Stellar network.
  *
  * @param offerId the id of the offer to be deleted
  * @param selling the asset being offered
  * @param buying the asset previously sought
  * @param price the price being offered
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-offer endpoint doc]]
  */
case class DeleteOfferOperation(override val offerId: Long,
                                selling: Asset, buying: Asset, price: Price,
                                sourceAccount: Option[PublicKeyOps] = None) extends ManageOfferOperation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(3) ++
      selling.encode ++
      buying.encode ++
      Encode.long(0) ++
      price.encode ++
      Encode.long(offerId)
}

/**
  * Updates an offer in the Stellar network.
  *
  * @param offerId the id of the offer to be modified
  * @param selling the asset and amount being offered
  * @param buying the asset sought
  * @param price the price being offered
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-offer endpoint doc]]
  */
case class UpdateOfferOperation(override val offerId: Long,
                                selling: Amount, buying: Asset, price: Price,
                                sourceAccount: Option[PublicKeyOps] = None) extends ManageOfferOperation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(3) ++
      selling.asset.encode ++
      buying.encode ++
      Encode.long(selling.units) ++
      price.encode ++
      Encode.long(offerId)
}

object ManageOfferOperation {
  def decode: State[Seq[Byte], ManageOfferOperation] = for {
    sellingAsset <- Asset.decode
    buyingAsset <- Asset.decode
    sellingUnits <- Decode.long
    price <- Price.decode
    offerId <- Decode.long
  } yield {
    if (offerId == 0) CreateOfferOperation(Amount(sellingUnits, sellingAsset), buyingAsset, price)
    else if (sellingUnits == 0) DeleteOfferOperation(offerId, sellingAsset, buyingAsset, price)
    else UpdateOfferOperation(offerId, Amount(sellingUnits, sellingAsset), buyingAsset, price)
  }
}

/**
  * Creates an offer that won’t consume a counter offer that exactly matches this offer.
  *
  * @param selling the total amount of tokens being offered
  * @param buying the asset being sought
  * @param price the price the offerer is willing to accept
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#create-passive-offer endpoint doc]]
  */
case class CreatePassiveOfferOperation(selling: Amount, buying: Asset, price: Price,
                                       sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(4) ++
      selling.asset.encode ++
      buying.encode ++
      Encode.long(selling.units) ++
      price.encode
}

object CreatePassiveOfferOperation {
  val decode: State[Seq[Byte], CreatePassiveOfferOperation] = for {
    sellingAsset <- Asset.decode
    buyingAsset <- Asset.decode
    sellingUnits <- Decode.long
    price <- Price.decode
  } yield CreatePassiveOfferOperation(Amount(sellingUnits, sellingAsset), buyingAsset, price)
}

/**
  * Modify an account, setting one or more options.
  *
  * @param inflationDestination the account's inflation destination
  * @param clearFlags issuer flags to be turned off
  * @param setFlags issuer flags to be turned on
  * @param masterKeyWeight the weight of the master key
  * @param lowThreshold the minimum weight required for low threshold operations
  * @param mediumThreshold the minimum weight required for medium threshold operations
  * @param highThreshold the minimum weight required for highthreshold operations
  * @param homeDomain the home domain used for reverse federation lookup
  * @param signer the key and weight of the signer for this account
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#set-options endpoint doc]]
  */
case class SetOptionsOperation(inflationDestination: Option[PublicKeyOps] = None,
                               clearFlags: Option[Set[IssuerFlag]] = None,
                               setFlags: Option[Set[IssuerFlag]] = None,
                               masterKeyWeight: Option[Int] = None,
                               lowThreshold: Option[Int] = None,
                               mediumThreshold: Option[Int] = None,
                               highThreshold: Option[Int] = None,
                               homeDomain: Option[String] = None,
                               signer: Option[Signer] = None,
                               sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(5) ++
      Encode.opt(inflationDestination) ++
      Encode.optInt(clearFlags.map(_.map(_.i) + 0).map(_.reduce(_ | _))) ++
      Encode.optInt(setFlags.map(_.map(_.i) + 0).map(_.reduce(_ | _))) ++
      Encode.optInt(masterKeyWeight) ++
      Encode.optInt(lowThreshold) ++
      Encode.optInt(mediumThreshold) ++
      Encode.optInt(highThreshold) ++
      Encode.optString(homeDomain) ++
      Encode.opt(signer)
}

object SetOptionsOperation {

  def decode: State[Seq[Byte], SetOptionsOperation] = for {
    inflationDestination <- Decode.opt(KeyPair.decode)
    clearFlags <- Decode.opt(Decode.int.map(IssuerFlags.from))
    setFlags <- Decode.opt(Decode.int.map(IssuerFlags.from))
    masterKeyWeight <- Decode.opt(Decode.int)
    lowThreshold <- Decode.opt(Decode.int)
    mediumThreshold <- Decode.opt(Decode.int)
    highThreshold <- Decode.opt(Decode.int)
    homeDomain <- Decode.opt(Decode.string)
    signer <- Decode.opt(Signer.decode)
  } yield SetOptionsOperation(inflationDestination, clearFlags, setFlags, masterKeyWeight, lowThreshold,
    mediumThreshold, highThreshold, homeDomain, signer)

}

sealed trait IssuerFlag {
  val i: Int
  val s: String
}

case object AuthorizationRequiredFlag extends IssuerFlag {
  val i = 0x1
  val s = "auth_required_flag"
}

case object AuthorizationRevocableFlag extends IssuerFlag {
  val i = 0x2
  val s = "auth_revocable_flag"
}

case object AuthorizationImmutableFlag extends IssuerFlag {
  val i = 0x4
  val s = "auth_immutable_flag"
}

object IssuerFlags {
  val all: Set[IssuerFlag] = Set(AuthorizationRequiredFlag, AuthorizationRevocableFlag, AuthorizationImmutableFlag)

  def apply(i: Int): Option[IssuerFlag] = all.find(_.i == i)

  def from(i: Int): Set[IssuerFlag] = all.filter { f => (i & f.i) == f.i }
}

/**
  * The source account is stating that it will trust the asset of the limit up to the amount of the limit.
  *
  * @param limit the asset to be trusted and the limit of that trust
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#change-trust endpoint doc]]
  */
case class ChangeTrustOperation(limit: IssuedAmount, sourceAccount: Option[PublicKeyOps] = None) extends Operation {
  override def encode: Stream[Byte] = super.encode ++ Encode.int(6) ++ limit.encode
}

object ChangeTrustOperation {
  def decode: State[Seq[Byte], ChangeTrustOperation] = IssuedAmount.decode.map(ChangeTrustOperation(_))
}

/**
  * Updates the “authorized” flag of an existing trust line. This is called by the issuer of the related asset.
  */
case class AllowTrustOperation(trustor: PublicKeyOps,
                               assetCode: String, // todo - make nonnativeasset
                               authorize: Boolean,
                               sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(7) ++
      trustor.encode ++
      (if (assetCode.length <= 4) Encode.int(1) ++ Encode.bytes(4, paddedByteArray(assetCode, 4))
      else Encode.int(2) ++ Encode.bytes(12, paddedByteArray(assetCode, 12))) ++
      Encode.bool(authorize)

}

object AllowTrustOperation {
  def decode: State[Seq[Byte], AllowTrustOperation] = for {
    trustor <- KeyPair.decode
    assetCodeLength <- Decode.int.map {
      case 1 => 4
      case 2 => 12
    }
    assetCode <- Decode.bytes(assetCodeLength).map(_.toArray).map(ByteArrays.paddedByteArrayToString)
    authorize <- Decode.bool
  } yield AllowTrustOperation(trustor, assetCode, authorize)
}

/**
  * Deletes account and transfers remaining balance to destination account.
  *
  * @param destination the account to receive the residual balances of the account to be merged
  * @param sourceAccount the account to be merged, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#account-merge endpoint doc]]
  */
case class AccountMergeOperation(destination: PublicKeyOps, sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {
  override def encode: Stream[Byte] = super.encode ++ Encode.int(8) ++ destination.encode
}

object AccountMergeOperation {
  def decode: State[Seq[Byte], AccountMergeOperation] = KeyPair.decode.map(AccountMergeOperation(_))
}

/**
  * Requests that the network runs the inflation process.
  *
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#inflation endpoint doc]]
  */
case class InflationOperation(sourceAccount: Option[PublicKeyOps] = None) extends Operation {
  override def encode: Stream[Byte] = super.encode ++ Encode.int(9)
}

sealed trait ManageDataOperation extends Operation {
  val name: String
}

/**
  * Deletes a Data Entry (name/value pair) for an account.
  *
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-data endpoint doc]]
  */
case class DeleteDataOperation(name: String, sourceAccount: Option[PublicKeyOps] = None) extends ManageDataOperation {
  override def encode: Stream[Byte] = super.encode ++ Encode.int(10) ++ Encode.string(name) ++ Encode.int(0)
}

/**
  * Creates or updates a Data Entry (name/value pair) for an account.
  *
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-data endpoint doc]]
  */
case class WriteDataOperation(name: String, value: String, sourceAccount: Option[PublicKeyOps] = None) extends ManageDataOperation {
  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(10) ++
      Encode.string(name) ++
      Encode.int(1) ++
      Encode.string(value)
}


object ManageDataOperation {
  def decode: State[Seq[Byte], ManageDataOperation] = for {
    name <- Decode.string
    value <- Decode.opt(Decode.string)
  } yield value match {
    case Some(v) => WriteDataOperation(name, v)
    case None => DeleteDataOperation(name)
  }

}

/**
  * Bumps forward the sequence number of the source account of the operation, allowing it to invalidate any transactions
  * with a smaller sequence number.
  *
  * @param bumpTo the number to increase the sequence number to
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#bump-sequence endpoint doc]]
  */
case class BumpSequenceOperation(bumpTo: Long,
                                 sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def encode: Stream[Byte] = super.encode ++ Encode.int(11) ++ Encode.long(bumpTo)
}

object BumpSequenceOperation {
  def decode: State[Seq[Byte], BumpSequenceOperation] = Decode.long.map(BumpSequenceOperation(_))
}

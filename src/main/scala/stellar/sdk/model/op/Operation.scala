package stellar.sdk.model.op

import java.nio.charset.StandardCharsets.UTF_8
import com.google.common.base.Charsets
import okio.ByteString
import org.json4s.JsonAST.{JArray, JObject}
import org.json4s.{DefaultFormats, Formats}
import org.stellar.xdr.Operation.OperationBody
import org.stellar.xdr.OperationType._
import org.stellar.xdr.RevokeSponsorshipOp.RevokeSponsorshipOpSigner
import org.stellar.xdr.{AccountFlags, AllowTrustOp, AssetCode, AssetCode12, AssetCode4, AssetType, BeginSponsoringFutureReservesOp, BumpSequenceOp, ChangeTrustOp, ClaimClaimableBalanceOp, ClawbackOp, CreateAccountOp, CreateClaimableBalanceOp, CreatePassiveSellOfferOp, DataValue, Int64, ManageBuyOfferOp, ManageDataOp, ManageSellOfferOp, PathPaymentStrictReceiveOp, PathPaymentStrictSendOp, PaymentOp, RevokeSponsorshipOp, RevokeSponsorshipType, SequenceNumber, SetOptionsOp, SetTrustLineFlagsOp, String32, String64, Uint32, XdrString, Operation => XOperation}
import stellar.sdk._
import stellar.sdk.model.Asset.parseAsset
import stellar.sdk.model._
import stellar.sdk.model.ledger._
import stellar.sdk.model.op.Operation.extractSource
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.util.ByteArrays.{base64, paddedByteArray}

/**
 * An Operation represents a change to the ledger. It is the action, as opposed to the effects resulting from that action.
 */
sealed trait Operation {
  val sourceAccount: Option[PublicKeyOps]

  def accountRequiringMemo: Option[AccountId] = None

  def xdr: XOperation = new XOperation.Builder()
    .sourceAccount(sourceAccount.map(_.toAccountId.muxedXdr).orNull)
    .body(bodyXdr)
    .build()

  def bodyXdr: OperationBody
}

object Operation {
  def decodeXdrString(xdr: String): Operation = decodeXdr(XOperation.decode(ByteString.decodeBase64(xdr)))

  def decodeXdr(xdr: XOperation): Operation =
    xdr.getBody.getDiscriminant match {
      case CREATE_ACCOUNT =>
        CreateAccountOperation.decode(xdr)
      case PAYMENT =>
        PaymentOperation.decode(xdr)
      case PATH_PAYMENT_STRICT_RECEIVE =>
        PathPaymentStrictReceiveOperation.decode(xdr)
      case MANAGE_SELL_OFFER =>
        ManageSellOfferOperation.decode(xdr)
      case CREATE_PASSIVE_SELL_OFFER =>
        CreatePassiveSellOfferOperation.decode(xdr)
      case SET_OPTIONS =>
        SetOptionsOperation.decode(xdr)
      case CHANGE_TRUST =>
        ChangeTrustOperation.decode(xdr)
      case ALLOW_TRUST =>
        AllowTrustOperation.decode(xdr)
      case ACCOUNT_MERGE =>
        AccountMergeOperation.decode(xdr)
      case INFLATION =>
        InflationOperation.decode(xdr)
      case MANAGE_DATA =>
        ManageDataOperation.decode(xdr)
      case BUMP_SEQUENCE =>
        BumpSequenceOperation.decode(xdr)
      case MANAGE_BUY_OFFER =>
        ManageBuyOfferOperation.decode(xdr)
      case PATH_PAYMENT_STRICT_SEND =>
        PathPaymentStrictSendOperation.decode(xdr)
      case CREATE_CLAIMABLE_BALANCE =>
        CreateClaimableBalanceOperation.decode(xdr)
      case CLAIM_CLAIMABLE_BALANCE =>
        ClaimClaimableBalanceOperation.decode(xdr)
      case BEGIN_SPONSORING_FUTURE_RESERVES =>
        BeginSponsoringFutureReservesOperation.decode(xdr)
      case END_SPONSORING_FUTURE_RESERVES =>
        EndSponsoringFutureReservesOperation.decode(xdr)
      case REVOKE_SPONSORSHIP =>
        RevokeSponsorshipOperation.decode(xdr)

    }

  def extractSource(xdr: XOperation): Option[PublicKey] =
    Option(xdr.getSourceAccount).map(_.getEd25519).map(_.getUint256).map(KeyPair.fromPublicKey)
}

object OperationDeserializer extends ResponseParser[Operation]({ o: JObject =>
  implicit val formats: Formats = DefaultFormats + ClaimantDeserializer

  def publicKey(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])

  def accountId(accountKey: String = "account") = AccountId(publicKey(accountKey).publicKey)

  def sourceAccount: Option[PublicKey] = Some(publicKey("source_account"))

  def nonNativeAsset = parseAsset("", o).asInstanceOf[NonNativeAsset]

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
    parseAsset(assetPrefix, o) match {
      case nna: NonNativeAsset => IssuedAmount(units, nna)
      case NativeAsset => NativeAmount(units)
    }
  }

  (o \ "type").extract[String] match {
    case "create_account" => CreateAccountOperation(accountId(), nativeAmount("starting_balance"), sourceAccount)
    case "payment" => PaymentOperation(accountId("to"), amount(), sourceAccount)
    case "path_payment" | "path_payment_strict_receive" =>
      val JArray(pathJs) = o \ "path"
      val path: List[Asset] = pathJs.map(a => parseAsset(obj = a))
      PathPaymentStrictReceiveOperation(amount("source_max", "source_"), accountId("to"), amount(), path, sourceAccount)
    case "path_payment_strict_send" =>
      val JArray(pathJs) = o \ "path"
      val path: List[Asset] = pathJs.map(a => parseAsset(obj = a))
      PathPaymentStrictSendOperation(amount(assetPrefix = "source_"), accountId("to"), amount(label = "destination_min"), path, sourceAccount)
    case "manage_offer" | "manage_sell_offer" =>
      (o \ "offer_id").extract[String].toLong match {
        case 0L => CreateSellOfferOperation(
          selling = amount(assetPrefix = "selling_"),
          buying = parseAsset("buying_", o),
          price = price(),
          sourceAccount = sourceAccount
        )
        case id =>
          val amnt = (o \ "amount").extract[String].toDouble
          if (amnt == 0.0) {
            DeleteSellOfferOperation(id, parseAsset("selling_", o), parseAsset("buying_", o), price(), sourceAccount)
          } else {
            UpdateSellOfferOperation(id, selling = amount(assetPrefix = "selling_"), buying = parseAsset("buying_", o),
              price = price(), sourceAccount)
          }
      }
    case "manage_buy_offer" =>
      (o \ "offer_id").extract[String].toLong match {
        case 0L => CreateBuyOfferOperation(
          buying = amount(assetPrefix = "buying_"),
          selling = parseAsset("selling_", o),
          price = price(),
          sourceAccount = sourceAccount
        )
        case id =>
          val amnt = (o \ "amount").extract[String].toDouble
          if (amnt == 0.0) {
            DeleteBuyOfferOperation(id, parseAsset("selling_", o), parseAsset("buying_", o), price(), sourceAccount)
          } else {
            UpdateBuyOfferOperation(id, parseAsset("selling_", o), amount(assetPrefix = "buying_"), price(), sourceAccount)
          }
      }
    case "create_passive_offer" | "create_passive_sell_offer" =>
      CreatePassiveSellOfferOperation(
        selling = amount(assetPrefix = "selling_"),
        buying = parseAsset("buying_", o),
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
        } yield Signer(StrKey.decodeFromString(key).asInstanceOf[SignerStrKey], weight),
        sourceAccount = sourceAccount
      )
    case "change_trust" =>
      ChangeTrustOperation(issuedAmount("limit"), sourceAccount)
    case "allow_trust" =>
      val asset: NonNativeAsset = nonNativeAsset
      val authorise = if ((o \ "authorize").extractOpt[Boolean].getOrElse(false)) 0x1 else 0
      val authoriseLiabilities = if ((o \ "authorize_to_maintain_liabilities").extractOpt[Boolean].getOrElse(false)) 0x2 else 0
      val trustLineFlags: Set[TrustLineFlag] = TrustLineFlags.from(authorise + authoriseLiabilities) // protocol >= 13
      AllowTrustOperation(
        publicKey("trustor"),
        asset.code,
        trustLineFlags,
        sourceAccount)
    case "account_merge" =>
      AccountMergeOperation(AccountId(KeyPair.fromAccountId((o \ "into").extract[String]).publicKey), sourceAccount)
    case "inflation" =>
      InflationOperation(sourceAccount)
    case "manage_data" =>
      val name = (o \ "name").extract[String]
      val value = (o \ "value").extract[String]
      value match {
        case "" => DeleteDataOperation(name, sourceAccount)
        case _ => WriteDataOperation(name, base64(value).toIndexedSeq, sourceAccount)
      }
    case "bump_sequence" =>
      BumpSequenceOperation((o \ "bump_to").extract[String].toLong, sourceAccount)
    case "create_claimable_balance" =>
      CreateClaimableBalanceOperation(
        amount = amount(),
        claimants = (o \ "claimants").extract[List[Claimant]],
        sourceAccount
      )
    case "claim_claimable_balance" =>
      ClaimClaimableBalanceOperation(
        id = ClaimableBalanceHashId(ByteString.decodeHex((o \ "balance_id").extract[String].drop(8))),
        sourceAccount
      )
    case "begin_sponsoring_future_reserves" =>
      BeginSponsoringFutureReservesOperation(
        sponsored = accountId("sponsored_id"),
        sourceAccount
      )
    case "end_sponsoring_future_reserves" =>
      EndSponsoringFutureReservesOperation(sourceAccount)
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
 * @param startingBalance    the amount of funds to send to it
 * @param sourceAccount      the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#create-account endpoint doc]]
 */
case class CreateAccountOperation(
  destinationAccount: AccountId,
  startingBalance: NativeAmount = Amount.lumens(1),
  sourceAccount: Option[PublicKeyOps] = None
) extends PayOperation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(CREATE_ACCOUNT)
    .createAccountOp(new CreateAccountOp.Builder()
      .destination(destinationAccount.xdr)
      .startingBalance(new Int64(startingBalance.units))
      .build())
    .build()
}

object CreateAccountOperation {
  def decode(xdr: XOperation): CreateAccountOperation = {
    val op = xdr.getBody.getCreateAccountOp
    CreateAccountOperation(
      destinationAccount = AccountId.decodeXdr(op.getDestination),
      startingBalance = NativeAmount(op.getStartingBalance.getInt64),
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Represents a payment from one account to another. This payment can be either a simple native asset payment or a
 * fiat asset payment.
 *
 * @param destinationAccount the recipient of the payment
 * @param amount             the amount to be paid
 * @param sourceAccount      the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#payment endpoint doc]]
 */
case class PaymentOperation(
  destinationAccount: AccountId,
  amount: Amount,
  sourceAccount: Option[PublicKeyOps] = None
) extends PayOperation {

  override def accountRequiringMemo: Option[AccountId] = Some(destinationAccount)

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(PAYMENT)
    .paymentOp(new PaymentOp.Builder()
      .amount(new Int64(amount.units))
      .asset(amount.asset.xdr)
      .destination(destinationAccount.muxedXdr)
      .build())
    .build()
}

object PaymentOperation {
  def decode(xdr: XOperation): PaymentOperation = {
    val op = xdr.getBody.getPaymentOp
    PaymentOperation(
      destinationAccount = AccountId.decodeXdr(op.getDestination),
      amount = Amount(op.getAmount.getInt64, Asset.decodeXdr(op.getAsset)),
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Represents a payment from one account to another through a path. This type of payment starts as one type of asset
 * and ends as another type of asset. There can be other assets that are traded into and out of along the path.
 * Suitable orders must exist on the relevant order books for this operation to be successful.
 * This operation specifies a precise amount to be received, and a maximum amount that can be sent.
 * If you need to specify the amount sent, use PathPaymentStrictSendOperation instead.
 *
 * @param sendMax            the maximum amount willing to be spent to effect the payment
 * @param destinationAccount the payment recipient
 * @param destinationAmount  the exact amount to be received
 * @param path               the intermediate assets to traverse (may be empty)
 * @param sourceAccount      the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#path-payment endpoint doc]]
 */
case class PathPaymentStrictReceiveOperation(sendMax: Amount,
  destinationAccount: AccountId,
  destinationAmount: Amount,
  path: Seq[Asset] = Nil,
  sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {

  override def accountRequiringMemo: Option[AccountId] = Some(destinationAccount)

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(PATH_PAYMENT_STRICT_RECEIVE)
    .pathPaymentStrictReceiveOp(new PathPaymentStrictReceiveOp.Builder()
      .destAmount(new Int64(destinationAmount.units))
      .destAsset(destinationAmount.asset.xdr)
      .destination(destinationAccount.muxedXdr)
      .path(path.map(_.xdr).toArray)
      .sendAsset(sendMax.asset.xdr)
      .sendMax(new Int64(sendMax.units))
      .build())
    .build()
}

object PathPaymentStrictReceiveOperation {
  def decode(xdr: XOperation): PathPaymentStrictReceiveOperation = {
    val op = xdr.getBody.getPathPaymentStrictReceiveOp
    PathPaymentStrictReceiveOperation(
      destinationAccount = AccountId.decodeXdr(op.getDestination),
      sendMax = Amount(op.getSendMax.getInt64, Asset.decodeXdr(op.getSendAsset)),
      destinationAmount = Amount(op.getDestAmount.getInt64, Asset.decodeXdr(op.getDestAsset)),
      path = op.getPath.map(Asset.decodeXdr),
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Represents a payment from one account to another through a path. This type of payment starts as one type of asset
 * and ends as another type of asset. There can be other assets that are traded into and out of along the path.
 * Suitable orders must exist on the relevant order books for this operation to be successful.
 * This operation specifies a precise amount to be sent, and a minimum amount that can be received.
 * If you need to specify the amount received, use PathPaymentStrictReceiveOperation instead.
 *
 * @param sendAmount         the amount to be spent to effect the payment
 * @param destinationAccount the payment recipient
 * @param destinationMin     the minimum amount that should be received
 * @param path               the intermediate assets to traverse (may be empty)
 * @param sourceAccount      the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/learn/concepts/list-of-operations.html#path-payment-strict-send endpoint doc]]
 */
case class PathPaymentStrictSendOperation(sendAmount: Amount,
  destinationAccount: AccountId,
  destinationMin: Amount,
  path: Seq[Asset] = Nil,
  sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {

  override def accountRequiringMemo: Option[AccountId] = Some(destinationAccount)

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(PATH_PAYMENT_STRICT_SEND)
    .pathPaymentStrictSendOp(new PathPaymentStrictSendOp.Builder()
      .destAsset(destinationMin.asset.xdr)
      .destination(destinationAccount.muxedXdr)
      .destMin(new Int64(destinationMin.units))
      .path(path.map(_.xdr).toArray)
      .sendAmount(new Int64(sendAmount.units))
      .sendAsset(sendAmount.asset.xdr)
      .build())
    .build()
}

object PathPaymentStrictSendOperation {
  def decode(xdr: XOperation): PathPaymentStrictSendOperation = {
    val op = xdr.getBody.getPathPaymentStrictSendOp
    PathPaymentStrictSendOperation(
      destinationAccount = AccountId.decodeXdr(op.getDestination),
      sendAmount = Amount(op.getSendAmount.getInt64, Asset.decodeXdr(op.getSendAsset)),
      destinationMin = Amount(op.getDestMin.getInt64, Asset.decodeXdr(op.getDestAsset)),
      path = op.getPath.map(Asset.decodeXdr),
      sourceAccount = extractSource(xdr)
    )
  }
}

sealed trait ManageSellOfferOperation extends Operation {
  val offerId: Long = 0
}

object ManageSellOfferOperation {
  def decode(xdr: XOperation): ManageSellOfferOperation = {
    val op = xdr.getBody.getManageSellOfferOp
    val id = op.getOfferID.getInt64
    val units = op.getAmount.getInt64
    val buyAsset = Asset.decodeXdr(op.getBuying)
    val sellAsset = Asset.decodeXdr(op.getSelling)
    val price = Price.decodeXdr(op.getPrice)
    val sourceAccount = extractSource(xdr)

    if (id == 0)
      CreateSellOfferOperation(Amount(units, sellAsset), buyAsset, price, sourceAccount)

    else if (units == 0)
      DeleteSellOfferOperation(id,sellAsset, buyAsset, price, sourceAccount)

    else
      UpdateSellOfferOperation(id, Amount(units, sellAsset), buyAsset, price, sourceAccount)
  }
}

/**
 * Creates a sell offer in the Stellar network.
 */
case class CreateSellOfferOperation(selling: Amount, buying: Asset, price: Price,
  sourceAccount: Option[PublicKeyOps] = None) extends ManageSellOfferOperation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(MANAGE_SELL_OFFER)
    .manageSellOfferOp(new ManageSellOfferOp.Builder()
      .amount(new Int64(selling.units))
      .buying(buying.xdr)
      .offerID(new Int64(offerId))
      .price(price.xdr)
      .selling(selling.asset.xdr)
      .build())
    .build()
}

/**
 * Deletes a sell offer in the Stellar network.
 *
 * @param offerId       the id of the offer to be deleted
 * @param selling       the asset being offered
 * @param buying        the asset previously sought
 * @param price         the price being offered
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-offer endpoint doc]]
 */
case class DeleteSellOfferOperation(override val offerId: Long,
  selling: Asset, buying: Asset, price: Price,
  sourceAccount: Option[PublicKeyOps] = None) extends ManageSellOfferOperation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(MANAGE_SELL_OFFER)
    .manageSellOfferOp(new ManageSellOfferOp.Builder()
      .amount(new Int64(0))
      .buying(buying.xdr)
      .offerID(new Int64(offerId))
      .price(price.xdr)
      .selling(selling.xdr)
      .build())
    .build()
}

/**
 * Updates a sell offer in the Stellar network.
 *
 * @param offerId       the id of the offer to be modified
 * @param selling       the asset and amount being offered
 * @param buying        the asset sought
 * @param price         the price being offered
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-offer endpoint doc]]
 */
case class UpdateSellOfferOperation(override val offerId: Long,
  selling: Amount, buying: Asset, price: Price,
  sourceAccount: Option[PublicKeyOps] = None) extends ManageSellOfferOperation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(MANAGE_SELL_OFFER)
    .manageSellOfferOp(new ManageSellOfferOp.Builder()
      .amount(new Int64(selling.units))
      .buying(buying.xdr)
      .offerID(new Int64(offerId))
      .price(price.xdr)
      .selling(selling.asset.xdr)
      .build())
    .build()
}

sealed trait ManageBuyOfferOperation extends Operation {
  val offerId: Long = 0
}

object ManageBuyOfferOperation {
  def decode(xdr: XOperation): ManageBuyOfferOperation = {
    val op = xdr.getBody.getManageBuyOfferOp
    val id = op.getOfferID.getInt64
    val units = op.getBuyAmount.getInt64
    val buyAsset = Asset.decodeXdr(op.getBuying)
    val sellAsset = Asset.decodeXdr(op.getSelling)
    val price = Price.decodeXdr(op.getPrice)
    val sourceAccount = extractSource(xdr)

    if (id == 0)
      CreateBuyOfferOperation(sellAsset, Amount(units, buyAsset), price, sourceAccount)

    else if (units == 0)
      DeleteBuyOfferOperation(id, sellAsset, buyAsset, price, sourceAccount)

    else
      UpdateBuyOfferOperation(id, sellAsset, Amount(units, buyAsset), price, sourceAccount)
  }
}

/**
 * Creates a buy offer in the Stellar network.
 */
case class CreateBuyOfferOperation(selling: Asset, buying: Amount, price: Price,
  sourceAccount: Option[PublicKeyOps] = None) extends ManageBuyOfferOperation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(MANAGE_BUY_OFFER)
    .manageBuyOfferOp(new ManageBuyOfferOp.Builder()
      .buyAmount(new Int64(buying.units))
      .buying(buying.asset.xdr)
      .offerID(new Int64(offerId))
      .price(price.xdr)
      .selling(selling.xdr)
      .build())
    .build()
}

/**
 * Deletes a buy offer in the Stellar network.
 *
 * @param offerId       the id of the offer to be deleted
 * @param selling       the asset previously offered
 * @param buying        the asset previously sought
 * @param price         the price being offered
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-offer endpoint doc]]
 */
case class DeleteBuyOfferOperation(override val offerId: Long,
  selling: Asset, buying: Asset, price: Price,
  sourceAccount: Option[PublicKeyOps] = None) extends ManageBuyOfferOperation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(MANAGE_BUY_OFFER)
    .manageBuyOfferOp(new ManageBuyOfferOp.Builder()
      .buyAmount(new Int64(0))
      .buying(buying.xdr)
      .offerID(new Int64(offerId))
      .price(price.xdr)
      .selling(selling.xdr)
      .build())
    .build()
}

/**
 * Updates a sell offer in the Stellar network.
 *
 * @param offerId       the id of the offer to be modified
 * @param selling       the asset offered
 * @param buying        the asset and amount being sought
 * @param price         the price being sought
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-offer endpoint doc]]
 */
case class UpdateBuyOfferOperation(override val offerId: Long,
  selling: Asset, buying: Amount, price: Price,
  sourceAccount: Option[PublicKeyOps] = None) extends ManageBuyOfferOperation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(MANAGE_BUY_OFFER)
    .manageBuyOfferOp(new ManageBuyOfferOp.Builder()
      .buyAmount(new Int64(buying.units))
      .buying(buying.asset.xdr)
      .offerID(new Int64(offerId))
      .price(price.xdr)
      .selling(selling.xdr)
      .build())
    .build()
}

/**
 * Creates an offer that won’t consume a counter offer that exactly matches this offer.
 *
 * @param selling       the total amount of tokens being offered
 * @param buying        the asset being sought
 * @param price         the price the offerer is willing to accept
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#create-passive-offer endpoint doc]]
 */
case class CreatePassiveSellOfferOperation(selling: Amount, buying: Asset, price: Price,
  sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(CREATE_PASSIVE_SELL_OFFER)
    .createPassiveSellOfferOp(new CreatePassiveSellOfferOp.Builder()
      .amount(new Int64(selling.units))
      .buying(buying.xdr)
      .price(price.xdr)
      .selling(selling.asset.xdr)
      .build())
    .build()

}

object CreatePassiveSellOfferOperation {
  def decode(xdr: XOperation): CreatePassiveSellOfferOperation = {
    val op = xdr.getBody.getCreatePassiveSellOfferOp
    CreatePassiveSellOfferOperation(
      selling = Amount(op.getAmount.getInt64, Asset.decodeXdr(op.getSelling)),
      buying = Asset.decodeXdr(op.getBuying),
      price = Price.decodeXdr(op.getPrice),
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Modify an account, setting one or more options.
 *
 * @param inflationDestination the account's inflation destination
 * @param clearFlags           issuer flags to be turned off
 * @param setFlags             issuer flags to be turned on
 * @param masterKeyWeight      the weight of the master key
 * @param lowThreshold         the minimum weight required for low threshold operations
 * @param mediumThreshold      the minimum weight required for medium threshold operations
 * @param highThreshold        the minimum weight required for highthreshold operations
 * @param homeDomain           the home domain used for reverse federation lookup
 * @param signer               the key and weight of the signer for this account
 * @param sourceAccount        the account effecting this operation, if different from the owning account of the transaction
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

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(SET_OPTIONS)
    .setOptionsOp(new SetOptionsOp.Builder()
      .clearFlags(clearFlags.map(f => new Uint32(IssuerFlags.flagsToInt(f))).orNull)
      .highThreshold(highThreshold.map(new Uint32(_)).orNull)
      .homeDomain(homeDomain.map(hd => new String32(new XdrString(hd))).orNull)
      .inflationDest(inflationDestination.map(_.toAccountId.xdr).orNull)
      .lowThreshold(lowThreshold.map(new Uint32(_)).orNull)
      .masterWeight(masterKeyWeight.map(new Uint32(_)).orNull)
      .medThreshold(mediumThreshold.map(new Uint32(_)).orNull)
      .setFlags(setFlags.map(f => new Uint32(IssuerFlags.flagsToInt(f))).orNull)
      .signer(signer.map(_.xdr).orNull)
      .build())
    .build()

}

object SetOptionsOperation {
  def decode(xdr: XOperation): SetOptionsOperation = {
    val op = xdr.getBody.getSetOptionsOp
    SetOptionsOperation(
      inflationDestination = Option(op.getInflationDest).map(AccountId.decodeXdr(_).publicKey),
      clearFlags = Option(op.getClearFlags).map(_.getUint32).map(IssuerFlags.from(_)),
      setFlags = Option(op.getSetFlags).map(_.getUint32).map(IssuerFlags.from(_)),
      masterKeyWeight = Option(op.getMasterWeight).map(_.getUint32),
      lowThreshold = Option(op.getLowThreshold).map(_.getUint32),
      mediumThreshold = Option(op.getMedThreshold).map(_.getUint32),
      highThreshold = Option(op.getHighThreshold).map(_.getUint32),
      homeDomain = Option(op.getHomeDomain).map(_.getString32.toString),
      signer = Option(op.getSigner).map(Signer.decodeXdr),
      sourceAccount = extractSource(xdr)
    )
  }
}

sealed trait TrustLineFlag {
  val i: Int
}

case object TrustLineAuthorized extends TrustLineFlag {
  val i = 0x1
}

case object TrustLineCanMaintainLiabilities extends TrustLineFlag {
  val i = 0x2
}

object TrustLineFlags {
  val all: Set[TrustLineFlag] = Set(TrustLineAuthorized, TrustLineCanMaintainLiabilities)

  // TODO (jem) - This duplicates methods in IssuerFlags. Combine.
  def apply(i: Int): Option[TrustLineFlag] = all.find(_.i == i)

  def from(i: Int): Set[TrustLineFlag] = all.filter { f => (i & f.i) == f.i }
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

case object AuthorizationClawbackEnabledFlag extends IssuerFlag {
  val i = 0x8
  val s = "auth_clawback_enabled_flag"
}

object IssuerFlags {
  val all: Set[IssuerFlag] = Set(
    AuthorizationRequiredFlag,
    AuthorizationRevocableFlag,
    AuthorizationImmutableFlag,
    AuthorizationClawbackEnabledFlag
  )

  def apply(i: Int): Option[IssuerFlag] = all.find(_.i == i)

  def from(i: Int): Set[IssuerFlag] = all.filter { f => (i & f.i) == f.i }

  def flagsToInt(flags: Set[IssuerFlag]): Int = (flags.map(_.i) + 0).reduce(_ | _)
}

/**
 * The source account is stating that it will trust the asset of the limit up to the amount of the limit.
 *
 * @param limit         the asset to be trusted and the limit of that trust
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#change-trust endpoint doc]]
 */
case class ChangeTrustOperation(
  limit: IssuedAmount,
  sourceAccount: Option[PublicKeyOps] = None
) extends Operation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(CHANGE_TRUST)
    .changeTrustOp(new ChangeTrustOp.Builder()
      .limit(new Int64(limit.units))
      .line(limit.asset.xdr)
      .build())
    .build()
}

object ChangeTrustOperation {
  def decode(xdr: XOperation): ChangeTrustOperation = {
    val op = xdr.getBody.getChangeTrustOp
    ChangeTrustOperation(
      limit = IssuedAmount(op.getLimit.getInt64, Asset.decodeXdr(op.getLine).asInstanceOf[NonNativeAsset]),
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Updates the “authorized” flag of an existing trust line. This is called by the issuer of the related asset.
 */
@deprecated("Use SetTrustLineFlagsOperation instead", "v0.20.0")
case class AllowTrustOperation(trustor: PublicKeyOps,
  assetCode: String,
  trustLineFlags: Set[TrustLineFlag],
  sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  def authorize: Boolean = trustLineFlags.contains(TrustLineAuthorized)

  def authorizeToMaintainLiabilities: Boolean = trustLineFlags.contains(TrustLineCanMaintainLiabilities)

  override def bodyXdr: OperationBody = {
    val asset = {
      val builder = new AssetCode.Builder()
      if (assetCode.length <= 4) {
        builder
          .assetCode4(new AssetCode4(paddedByteArray(assetCode, 4)))
          .discriminant(AssetType.ASSET_TYPE_CREDIT_ALPHANUM4)
      } else {
        builder
          .assetCode12(new AssetCode12(paddedByteArray(assetCode, 12)))
          .discriminant(AssetType.ASSET_TYPE_CREDIT_ALPHANUM12)
      }
      builder.build()
    }
    new OperationBody.Builder()
      .discriminant(ALLOW_TRUST)
      .allowTrustOp(new AllowTrustOp.Builder()
        .asset(asset)
        .authorize(new Uint32(trustLineFlags.map(_.i).sum))
        .trustor(trustor.toAccountId.xdr)
        .build())
      .build()
  }
}

object AllowTrustOperation {
  def decode(xdr: XOperation): AllowTrustOperation = {
    val op = xdr.getBody.getAllowTrustOp
    AllowTrustOperation(
      trustor = KeyPair.fromPublicKey(op.getTrustor.getAccountID.getEd25519.getUint256),
      assetCode = Asset.decodeCode(op.getAsset),
      trustLineFlags = TrustLineFlags.from(op.getAuthorize.getUint32),
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Deletes account and transfers remaining balance to destination account.
 *
 * @param destination   the account to receive the residual balances of the account to be merged
 * @param sourceAccount the account to be merged, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#account-merge endpoint doc]]
 */
case class AccountMergeOperation(destination: AccountId, sourceAccount: Option[PublicKeyOps] = None) extends PayOperation {
  override def accountRequiringMemo: Option[AccountId] = Some(destination)

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(ACCOUNT_MERGE)
    .destination(destination.muxedXdr)
    .build()
}

object AccountMergeOperation {
  def decode(xdr: XOperation): AccountMergeOperation = {
    AccountMergeOperation(
      destination = AccountId.decodeXdr(xdr.getBody.getDestination),
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Requests that the network runs the inflation process. As of Stellar Core v12, this operation does nothing.
 *
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#inflation endpoint doc]]
 */
case class InflationOperation(sourceAccount: Option[PublicKeyOps] = None) extends Operation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(INFLATION)
    .build()
}

object InflationOperation {
  def decode(op: XOperation): InflationOperation = InflationOperation(
    sourceAccount = Option(op.getSourceAccount).map(_.getEd25519).map(_.getUint256).map(KeyPair.fromPublicKey)
  )
}

sealed trait ManageDataOperation extends Operation {
  val name: String
}

object ManageDataOperation {
  def decode(xdr: XOperation): ManageDataOperation = {
    val op = xdr.getBody.getManageDataOp
    val name = op.getDataName.getString64.toString
    val value = Option(op.getDataValue).map(_.getDataValue)
    value match {
      case Some(v) => WriteDataOperation(name, v, extractSource(xdr))
      case None => DeleteDataOperation(name, extractSource(xdr))
    }
  }
}

/**
 * Deletes a Data Entry (name/value pair) for an account.
 *
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-data endpoint doc]]
 */
case class DeleteDataOperation(name: String, sourceAccount: Option[PublicKeyOps] = None) extends ManageDataOperation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(MANAGE_DATA)
    .manageDataOp(new ManageDataOp.Builder()
      .dataName(new String64(new XdrString(name.getBytes(Charsets.UTF_8))))
      .build())
    .build()
}

/**
 * Creates or updates a Data Entry (name/value pair) for an account.
 *
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#manage-data endpoint doc]]
 */
case class WriteDataOperation(name: String, value: Seq[Byte], sourceAccount: Option[PublicKeyOps] = None) extends ManageDataOperation {

  require(name.getBytes(UTF_8).length <= 64, s"name cannot be greater than 64 bytes, was ${name.length}")
  require(value.length <= 64 && value.nonEmpty, s"value must be non-empty and cannot be greater than 64 bytes, was ${value.length}")

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(MANAGE_DATA)
    .manageDataOp(new ManageDataOp.Builder()
      .dataName(new String64(new XdrString(name)))
      .dataValue(new DataValue(value.toArray))
      .build())
    .build()
}

object WriteDataOperation {
  def apply(name: String, value: String): WriteDataOperation =
    WriteDataOperation(name, value.getBytes(UTF_8).toIndexedSeq)

  def apply(name: String, value: String, sourceAccount: Option[PublicKeyOps]): WriteDataOperation =
    WriteDataOperation(name, value.getBytes(UTF_8).toIndexedSeq, sourceAccount)
}

/**
 * Bumps forward the sequence number of the source account of the operation, allowing it to invalidate any transactions
 * with a smaller sequence number.
 *
 * @param bumpTo        the number to increase the sequence number to
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#bump-sequence endpoint doc]]
 */
case class BumpSequenceOperation(bumpTo: Long,
  sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(BUMP_SEQUENCE)
    .bumpSequenceOp(new BumpSequenceOp.Builder()
      .bumpTo(new SequenceNumber(new Int64(bumpTo)))
      .build())
    .build()
}

object BumpSequenceOperation {
  def decode(xdr: XOperation): BumpSequenceOperation = {
    val op = xdr.getBody.getBumpSequenceOp
    BumpSequenceOperation(
      bumpTo = op.getBumpTo.getSequenceNumber.getInt64,
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Creates a payment reservation, referred to as a claimable balance.
 *
 * @param amount        the reserved payment amount
 * @param claimants     the accounts who can claim this payment, along with the requirements for doing so.
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
 * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#create-claimable-balance endpoint doc]]
 */
case class CreateClaimableBalanceOperation(
  amount: Amount,
  claimants: List[Claimant],
  sourceAccount: Option[PublicKeyOps] = None
) extends Operation {
  require(claimants.nonEmpty && claimants.size <= 10)

  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(CREATE_CLAIMABLE_BALANCE)
    .createClaimableBalanceOp(new CreateClaimableBalanceOp.Builder()
      .amount(new Int64(amount.units))
      .asset(amount.asset.xdr)
      .claimants(claimants.map(_.xdr).toArray)
      .build())
    .build()
}

object CreateClaimableBalanceOperation {
  def decode(xdr: XOperation): CreateClaimableBalanceOperation = {
    val op = xdr.getBody.getCreateClaimableBalanceOp
    CreateClaimableBalanceOperation(
      amount = Amount(op.getAmount.getInt64, Asset.decodeXdr(op.getAsset)),
      claimants = op.getClaimants.map(Claimant.decodeXdr).toList,
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Claims a previously created claimable balance.
 *
 * @param id            the id of the claimable balance entry to attempt to claim.
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction.
 */
case class ClaimClaimableBalanceOperation(
  id: ClaimableBalanceId,
  sourceAccount: Option[PublicKeyOps] = None
) extends Operation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(CLAIM_CLAIMABLE_BALANCE)
    .claimClaimableBalanceOp(new ClaimClaimableBalanceOp.Builder()
      .balanceID(id.xdr)
      .build())
    .build()
}

object ClaimClaimableBalanceOperation {
  def decode(xdr: XOperation): ClaimClaimableBalanceOperation = {
    val op = xdr.getBody.getClaimClaimableBalanceOp
    ClaimClaimableBalanceOperation(
      id = ClaimableBalanceId.decodeXdr(op.getBalanceID),
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * Begin sponsoring (paying for) any reserve that the sponsored account would have to pay.
 *
 * @param sponsored     the id of the account that is being sponsored.
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction.
 */
case class BeginSponsoringFutureReservesOperation(
  sponsored: AccountId,
  sourceAccount: Option[PublicKeyOps] = None
) extends Operation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(BEGIN_SPONSORING_FUTURE_RESERVES)
    .beginSponsoringFutureReservesOp(new BeginSponsoringFutureReservesOp.Builder()
      .sponsoredID(sponsored.xdr)
      .build())
    .build()
}

object BeginSponsoringFutureReservesOperation {
  def decode(xdr: XOperation): BeginSponsoringFutureReservesOperation = {
    val op = xdr.getBody.getBeginSponsoringFutureReservesOp
    BeginSponsoringFutureReservesOperation(
      sponsored = AccountId.decodeXdr(op.getSponsoredID),
      sourceAccount = extractSource(xdr)
    )
  }
}

/**
 * End sponsoring all reserves from the source account.
 */
case class EndSponsoringFutureReservesOperation(
  sourceAccount: Option[PublicKeyOps] = None
) extends Operation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(END_SPONSORING_FUTURE_RESERVES)
    .build()
}

object EndSponsoringFutureReservesOperation {
  def decode(xdr: XOperation): EndSponsoringFutureReservesOperation =
    EndSponsoringFutureReservesOperation(extractSource(xdr))
}

sealed trait RevokeSponsorshipOperation extends Operation

object RevokeSponsorshipOperation {
  def decode(xdr: XOperation): RevokeSponsorshipOperation = {
    val op = xdr.getBody.getRevokeSponsorshipOp
    op.getDiscriminant match {
      case RevokeSponsorshipType.REVOKE_SPONSORSHIP_LEDGER_ENTRY =>
        val ledgerKey = LedgerKey.decodeXdr(op.getLedgerKey)
        ledgerKey match {
          case k: AccountKey =>
            RevokeAccountSponsorshipOperation(k, extractSource(xdr))
          case k: ClaimableBalanceKey =>
            RevokeClaimableBalanceSponsorshipOperation(k, extractSource(xdr))
          case k: DataKey =>
            RevokeDataSponsorshipOperation(k, extractSource(xdr))
          case k: OfferKey =>
            RevokeOfferSponsorshipOperation(k, extractSource(xdr))
          case k: TrustLineKey =>
            RevokeTrustLineSponsorshipOperation(k, extractSource(xdr))
        }

      case RevokeSponsorshipType.REVOKE_SPONSORSHIP_SIGNER =>
        RevokeSignerSponsorshipOperation(
          accountId = AccountId.decodeXdr(op.getSigner.getAccountID),
          signerKey = SignerStrKey.decodeXdr(op.getSigner.getSignerKey),
          sourceAccount = extractSource(xdr)
        )
    }
  }
}

/**
 * Remove the sponsorship of ledger account entries.
 *
 * @param revokeAccountKey the key for the account ledger entry to have its sponsorship removed.
 * @param sourceAccount    the account effecting this operation, if different from the owning account of the transaction.
 */
case class RevokeAccountSponsorshipOperation(
  revokeAccountKey: AccountKey,
  sourceAccount: Option[PublicKeyOps] = None
) extends RevokeSponsorshipOperation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(REVOKE_SPONSORSHIP)
    .revokeSponsorshipOp(new RevokeSponsorshipOp.Builder()
      .discriminant(RevokeSponsorshipType.REVOKE_SPONSORSHIP_LEDGER_ENTRY)
      .ledgerKey(revokeAccountKey.xdr)
      .build())
    .build()
}

/**
 * Remove the sponsorship of ledger claimable balance entries.
 *
 * @param revokeClaimableBalanceKey the key for the claimable balance entry to have its sponsorship removed.
 * @param sourceAccount             the account effecting this operation, if different from the owning account of the transaction.
 */
case class RevokeClaimableBalanceSponsorshipOperation(
  revokeClaimableBalanceKey: ClaimableBalanceKey,
  sourceAccount: Option[PublicKeyOps] = None
) extends RevokeSponsorshipOperation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(REVOKE_SPONSORSHIP)
    .revokeSponsorshipOp(new RevokeSponsorshipOp.Builder()
      .discriminant(RevokeSponsorshipType.REVOKE_SPONSORSHIP_LEDGER_ENTRY)
      .ledgerKey(revokeClaimableBalanceKey.xdr)
      .build())
    .build()
}

/**
 * Remove the sponsorship of ledger data entries.
 *
 * @param revokeDataKey the key for the data ledger entry to have its sponsorship removed.
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction.
 */
case class RevokeDataSponsorshipOperation(
  revokeDataKey: DataKey,
  sourceAccount: Option[PublicKeyOps] = None
) extends RevokeSponsorshipOperation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(REVOKE_SPONSORSHIP)
    .revokeSponsorshipOp(new RevokeSponsorshipOp.Builder()
      .discriminant(RevokeSponsorshipType.REVOKE_SPONSORSHIP_LEDGER_ENTRY)
      .ledgerKey(revokeDataKey.xdr)
      .build())
    .build()
}

/**
 * Remove the sponsorship of ledger offer entries.
 *
 * @param revokeOfferKey the key for the offer ledger entry to have its sponsorship removed.
 * @param sourceAccount  the account effecting this operation, if different from the owning account of the transaction.
 */
case class RevokeOfferSponsorshipOperation(
  revokeOfferKey: OfferKey,
  sourceAccount: Option[PublicKeyOps] = None
) extends RevokeSponsorshipOperation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(REVOKE_SPONSORSHIP)
    .revokeSponsorshipOp(new RevokeSponsorshipOp.Builder()
      .discriminant(RevokeSponsorshipType.REVOKE_SPONSORSHIP_LEDGER_ENTRY)
      .ledgerKey(revokeOfferKey.xdr)
      .build())
    .build()
}

/**
 * Remove a signer's sponsorship of an account.
 *
 * @param accountId     the id for the account to have a sponsor removed.
 * @param signerKey     the signer's key of the sponsor to be removed.
 * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction.
 */
case class RevokeSignerSponsorshipOperation(
  accountId: AccountId,
  signerKey: SignerStrKey,
  sourceAccount: Option[PublicKeyOps] = None
) extends RevokeSponsorshipOperation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(REVOKE_SPONSORSHIP)
    .revokeSponsorshipOp(new RevokeSponsorshipOp.Builder()
      .discriminant(RevokeSponsorshipType.REVOKE_SPONSORSHIP_SIGNER)
      .signer(new RevokeSponsorshipOpSigner.Builder()
        .accountID(accountId.xdr)
        .signerKey(signerKey.signerXdr)
        .build())
      .build())
    .build()
}

/**
 * Remove the sponsorship of ledger trustline entries.
 *
 * @param revokeTrustLineKey the key for the trustline ledger entry to have its sponsorship removed.
 * @param sourceAccount      the account effecting this operation, if different from the owning account of the transaction.
 */
case class RevokeTrustLineSponsorshipOperation(
  revokeTrustLineKey: TrustLineKey,
  sourceAccount: Option[PublicKeyOps] = None
) extends RevokeSponsorshipOperation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(REVOKE_SPONSORSHIP)
    .revokeSponsorshipOp(new RevokeSponsorshipOp.Builder()
      .discriminant(RevokeSponsorshipType.REVOKE_SPONSORSHIP_LEDGER_ENTRY)
      .ledgerKey(revokeTrustLineKey.xdr)
      .build())
    .build()
}

/**
 * Claw back an asset from an account.
 */
case class ClawBackOperation(
  from: AccountId,
  amount: IssuedAmount,
  sourceAccount: Option[PublicKeyOps] = None
) extends Operation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(CLAWBACK)
    .clawbackOp(new ClawbackOp.Builder()
      .amount(new Int64(amount.units))
      .from(from.muxedXdr)
      .asset(amount.asset.xdr)
      .build())
    .build()
}

/**
 * Set flags on the trustline for an account.
 *
case class SetTrustLineFlagsOperation(
  asset: NonNativeAsset,
  trustor: PublicKeyOps,
//  setFlags: Set[]

  sourceAccount: Option[PublicKeyOps] = None
) extends Operation {
  override def bodyXdr: OperationBody = new OperationBody.Builder()
    .discriminant(SET_TRUST_LINE_FLAGS)
    .setTrustLineFlagsOp(new SetTrustLineFlagsOp.Builder()
      .asset(asset.xdr)
      .trustor(trustor.toAccountId.xdr)
      .setFlags(AccountFlags.AUTH_CLAWBACK_ENABLED_FLAG)
      .clearFlags()
      .discriminant(RevokeSponsorshipType.REVOKE_SPONSORSHIP_SIGNER)
      .signer(new RevokeSponsorshipOpSigner.Builder()
        .accountID(accountId.xdr)
        .signerKey(signerKey.signerXdr)
        .build())
      .build())
    .build()
}
*/
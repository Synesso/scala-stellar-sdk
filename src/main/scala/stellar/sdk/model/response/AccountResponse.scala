package stellar.sdk.model.response

import java.nio.charset.StandardCharsets.UTF_8

import org.json4s.{DefaultFormats, Formats}
import org.json4s.JsonAST.{JArray, JObject}
import stellar.sdk._
import stellar.sdk.model.Amount.toBaseUnits
import stellar.sdk.model._
import stellar.sdk.util.ByteArrays

case class AccountResponse(
  id: PublicKey,
  lastSequence: Long,
  subEntryCount: Int,
  thresholds: Thresholds,
  authRequired: Boolean,
  authRevocable: Boolean,
  balances: List[Balance],
  signers: List[Signer],
  sponsor: Option[PublicKey],
  reservesSponsored: Int,
  reservesSponsoring: Int,
  data: Map[String, Array[Byte]]
) {

  def toAccount: Account = Account(AccountId(id.publicKey), lastSequence + 1)

  def decodedData: Map[String, String] = data.map { case (k, v) => k -> new String(v, UTF_8) }
}

object AccountRespDeserializer extends ResponseParser[AccountResponse]({ o: JObject =>
  implicit val formats: Formats = DefaultFormats
  val id = KeyPair.fromAccountId((o \ "id").extract[String])
  val seq = (o \ "sequence").extract[String].toLong
  val subEntryCount = (o \ "subentry_count").extract[Int]
  val lowThreshold = (o \ "thresholds" \ "low_threshold").extract[Int]
  val mediumThreshold = (o \ "thresholds" \ "med_threshold").extract[Int]
  val highThreshold = (o \ "thresholds" \ "high_threshold").extract[Int]
  val authRequired = (o \ "flags" \ "auth_required").extract[Boolean]
  val authRevocable = (o \ "flags" \ "auth_revocable").extract[Boolean]
  val JArray(jsBalances) = o \ "balances"
  val balances = jsBalances.map {
    case balObj: JObject =>
      val units = toBaseUnits((balObj \ "balance").extract[String].toDouble).get
      val amount = (balObj \ "asset_type").extract[String] match {
        case "credit_alphanum4" =>
          Amount(units, IssuedAsset4(
            code = (balObj \ "asset_code").extract[String],
            issuer = KeyPair.fromAccountId((balObj \ "asset_issuer").extract[String])
          ))
        case "credit_alphanum12" =>
          Amount(units, IssuedAsset12(
            code = (balObj \ "asset_code").extract[String],
            issuer = KeyPair.fromAccountId((balObj \ "asset_issuer").extract[String])
          ))
        case "native" => NativeAmount(units)
        case t => throw new RuntimeException(s"Unrecognised asset type: $t")
      }
      val limit = (balObj \ "limit").extractOpt[String].map(BigDecimal(_)).map(toBaseUnits).map(_.get)
      val buyingLiabilities = toBaseUnits(BigDecimal((balObj \ "buying_liabilities").extract[String])).get
      val sellingLiabilities = toBaseUnits(BigDecimal((balObj \ "selling_liabilities").extract[String])).get
      val authorised = (balObj \ "is_authorized").extractOpt[Boolean].getOrElse(false)
      val authorisedToMaintainLiabilities = (balObj \ "is_authorized_to_maintain_liabilities")
        .extractOpt[Boolean].getOrElse(false)
      val sponsor = (balObj \ "sponsor").extractOpt[String].map(KeyPair.fromAccountId)

      Balance(amount, limit, buyingLiabilities, sellingLiabilities, authorised, authorisedToMaintainLiabilities, sponsor)
    case _ => throw new RuntimeException(s"Expected js object at 'balances'")
  }
  val JArray(jsSigners) = o \ "signers"
  val signers = jsSigners.map {
    case signerObj: JObject =>
      val key = StrKey.decodeFromString((signerObj \ "key").extract[String]).asInstanceOf[SignerStrKey]
      val weight = (signerObj \ "weight").extract[Int]
      Signer(key, weight)
    case _ => throw new RuntimeException(s"Expected js object at 'signers'")
  }
  val JObject(dataFields) = o \ "data"
  val data = dataFields.map { case (k, v) => k -> ByteArrays.base64(v.extract[String]) }.toMap
  val sponsor = (o \ "sponsor").extractOpt[String].map(KeyPair.fromAccountId)
  val reservesSponsored = (o \ "num_sponsored").extractOpt[Int].getOrElse(0)
  val reservesSponsoring = (o \ "num_sponsoring").extractOpt[Int].getOrElse(0)

  AccountResponse(id, seq, subEntryCount, Thresholds(lowThreshold, mediumThreshold, highThreshold),
    authRequired, authRevocable, balances, signers, sponsor, reservesSponsored,
    reservesSponsoring, data)

})

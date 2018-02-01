package stellar.sdk.resp

import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JArray, JObject}
import stellar.sdk._
import org.json4s.DefaultFormats

// e.g. https://horizon-testnet.stellar.org/accounts/GDGUM5IKSJIFQEHXAWGQD2IWT2OUD6YTY4U7D7SSZLO23BVWHAFL54YN
case class AccountResp(id: String,
                       sequence: Long,
                       subEntryCount: Int,
                       thresholds: Thresholds,
                       authRequired: Boolean,
                       authRevocable: Boolean,
                       balances: List[Amount],
                       signers: List[Signer])

class AccountRespDeserializer extends CustomSerializer[AccountResp](format => ({
  case o: JObject =>
    implicit val formats = DefaultFormats
    val id = (o \ "id").extract[String]
    // todo - account id is just duplicate of id?
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
        val units = (balObj \ "balance").extract[String].toDouble
        // todo - asset type
      Amount.lumens(units)
    }
    val JArray(jsSigners) = o \ "signers"
    val signers = jsSigners.map {
      case signerObj: JObject =>
        val publicKey = KeyPair.fromAccountId((signerObj \ "public_key").extract[String])
        val weight = (signerObj \ "weight").extract[Int]
        // todo - key is just duplicate of publicKey?
        // todo - type
      Signer(publicKey, weight)
    }
    // todo - data

    AccountResp(id, seq, subEntryCount, Thresholds(lowThreshold, mediumThreshold, highThreshold), authRequired,
      authRevocable, balances, signers)

  }, PartialFunction.empty)
)

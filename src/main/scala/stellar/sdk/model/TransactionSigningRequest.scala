package stellar.sdk.model

import okhttp3.HttpUrl
import org.json4s.CustomSerializer
import stellar.sdk.inet.HorizonAccess
import stellar.sdk.model.response.TransactionPostResponse
import stellar.sdk.util.DoNothingNetwork
import stellar.sdk.{KeyPair, Network, PublicKey, PublicNetwork}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/** A request to a transaction to be signed.
 *
 * @param transaction The signed transaction to be encoded
 * @param form The additional information required by the user in the form `form_label -> (txrep_field, form_hint)`
 * @param callback the uri to post the transaction to after signing
 * @param pubkey the public key associated with the signer who should sign
 * @param message an optional message for displaying to the user
 * @param networkPassphrase the passphrase of the target network, if it's not the public/main network
 * @see See [[https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0007.md#operation-tx SEP-0007 for full specification]]
 */
case class TransactionSigningRequest(
  transaction: SignedTransaction,
  form: Map[String, (String, String)] = Map.empty,
  callback: Option[HttpUrl] = None,
  pubkey: Option[PublicKey] = None,
  message: Option[String] = None,
  networkPassphrase: Option[String] = None
) {
  form.keys.foreach(validateFormLabel)
  message.foreach(m => require(m.length <= 300, "Message must not exceed 300 characters"))

  def toUrl: String = {
    val encodedForm = if (form.isEmpty) None else {
      val fieldToLabel = form.map { case (label, (txRepField, _)) => s"$txRepField:$label" }.mkString(",")
      val labelToHint = form.map { case (label, (_, hint)) => s"$label:$hint" }.mkString(",")
      Some("replace" -> s"$fieldToLabel;$labelToHint")
    }

    val paramMap = Map(
      "xdr" -> Some(transaction.encodeXDR),
      "callback" -> callback.map(c => s"url:$c"),
      "pubkey" -> pubkey.map(_.accountId),
      "msg" -> message,
      "network_passphrase" -> networkPassphrase
    ).filter(_._2.isDefined).foldLeft(Map.empty[String, String]) {
      case (m, (k, Some(v))) => m.updated(k, v)
      case (m, _) =>            m
    }

    val params = List(encodedForm).flatten.foldLeft(paramMap) { case (map, (key, value)) =>
      map.updated(key, value)
    }

    params.foldLeft(new HttpUrl.Builder()
      .scheme("http")
      .host("z")
      .addPathSegment("tx")) { case (builder, (key, value)) =>
      builder.addQueryParameter(key, value)
    }.toString()
      .replaceFirst("http://z/", "web+stellar:")
  }

  private def validateFormLabel(label: String) {
    require(!label.trim.isEmpty, "Form label cannot be empty")
    require(!label.contains(":"), "Form label cannot contain ':'")
  }
}

object TransactionSigningRequest {

  def apply(url: String): TransactionSigningRequest = {
    require(url.startsWith("web+stellar:tx?"))
    val httpUrl = HttpUrl.parse(url.replaceFirst("web\\+stellar:", "http://a/"))
    val form = Option(httpUrl.queryParameter("replace"))
      .map(splitOnce(_, ";"))
      .map { case (ftl, lth) =>
        val ftlMap = ftl.split(",").map(splitOnce(_, ":", true)).toMap
        val lthMap = lth.split(",").map(splitOnce(_, ":")).toMap
        if (ftlMap.keySet != lthMap.keys) {
          throw new IllegalArgumentException(s"Field `replace` has mismatched form labels." +
            s"[left=${ftlMap.keySet}][right=${lthMap.keySet}][param=${httpUrl.queryParameter("replace")}]")
        }
        ftlMap.map { case (field, label) => field -> (label, lthMap(field)) }
      }
      .getOrElse(Map.empty)

    val callback = Option(httpUrl.queryParameter("callback"))
      .map { callback =>
        Option(callback)
          .filter(_.startsWith("url:"))
          .map(_.drop(4))
          .flatMap(c => Option(HttpUrl.parse(c)))
          .getOrElse(throw new IllegalArgumentException(s"Invalid callback: [url=$url][callback=$callback]"))
      }

    val pubKey = Option(httpUrl.queryParameter("pubkey"))
        .map(KeyPair.fromAccountId)

    val message = Option(httpUrl.queryParameter("msg"))

    val network = Option(httpUrl.queryParameter("network_passphrase"))
        // TODO (jem) - The transaction objects should take a strongly typed network passphrase object.
        .map(p => new DoNothingNetwork(p))

    Option(httpUrl.queryParameter("xdr"))
        .map(SignedTransaction.decodeXDR(_)(network.getOrElse(PublicNetwork)))
        .map(TransactionSigningRequest(_, form, callback, pubKey, message, network.map(_.passphrase)))
      .getOrElse(throw new IllegalArgumentException(s"Invalid url: [url=$url]"))
  }

  private def splitOnce(str: String, sep: String, reverse: Boolean = false): (String, String) = {
    val split: List[String] =
      if (reverse) str.reverse.split(sep).toList.map(_.reverse)
      else str.split(sep).toList

    split match {
      case h +: ts => h -> ts.mkString(sep)
      case _ => throw new IllegalArgumentException(s"Cannot split by separator [str=$str][sep=$sep]")
    }
  }

}
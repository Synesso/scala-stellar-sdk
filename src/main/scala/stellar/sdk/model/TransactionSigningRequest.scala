package stellar.sdk.model

import java.net.URLEncoder

import okhttp3.HttpUrl
import okio.{Buffer, ByteString}
import stellar.sdk.model.domain.DomainInfo
import stellar.sdk.util.DoNothingNetwork
import stellar.sdk.{KeyPair, PublicKey, PublicNetwork}

import scala.concurrent.{ExecutionContext, Future}

/** A request to a transaction to be signed.
 *
 * @param transaction The signed transaction to be encoded
 * @param form The additional information required by the user in the form `form_label -> (txrep_field, form_hint)`
 * @param callback the uri to post the transaction to after signing
 * @param pubkey the public key associated with the signer who should sign
 * @param message an optional message for displaying to the user
 * @param networkPassphrase the passphrase of the target network, if it's not the public/main network
 * @see See [[https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0007.md#operation-tx|SEP-0007]] for full specification
 */
case class TransactionSigningRequest(
  transaction: SignedTransaction,
  form: Map[String, (String, String)] = Map.empty,
  callback: Option[HttpUrl] = None,
  pubkey: Option[PublicKey] = None,
  message: Option[String] = None,
  networkPassphrase: Option[String] = None,
  signature: Option[DomainSignature] = None
) {

  form.keys.foreach(validateFormLabel)
  message.foreach(m => require(m.length <= 300, "Message must not exceed 300 characters"))

  /**
   * Sign the signing request with the given key.
   * @param fqdn The fully-qualified domain name that contains the TOML file specifying the signer's public key.
   * @param key The private key associated with the declared public key.
   * @return this request with a signature populated. No attempt is made to validate that the provided `key` matches
   *         the public key declared in the TOML file of the `fqdn` under `URI_REQUEST_SIGNING_KEY`. No structural
   *         validation is performed on `fqdn` String.
   * @see https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0007.md#request-signing for more info.
   */
  def sign(fqdn: String, key: KeyPair): TransactionSigningRequest = {
    val payload = baseSigningPayload(fqdn)
    val signature = key.sign(payload.toByteArray)
    this.copy(signature = Some(DomainSignature(fqdn, new ByteString(signature.data))))
  }

  /**
   * If a signature is present, executes a round-trip to the domain info of the declared domain to fetch the signing
   * public key and checks the signature against that key.

   * @return NoSignaturePresent if there is no signature declared
   *         InvalidSignature if the domain cannot be reached, has no TOML, does not declare a signing key or the key
   *          used does not match the declared key
   *         ValidSignature if the signature is valid.
   */
  def validateSignature(
    useHttps: Boolean = true,
    port: Int = 443
  )(implicit ec: ExecutionContext): Future[SignatureValidation] = {
    signature.map { case DomainSignature(domain, sigBytes) =>
      DomainInfo.forDomain(domain, useHttps, port)
        .recover { case _ => None }
        .map(_.flatMap(_.uriRequestSigningKey) match {
        case None => InvalidSignature
        case Some(signingKey) =>
          val payload = baseSigningPayload(domain)
          if (signingKey.verify(payload.toByteArray, sigBytes.toByteArray)) ValidSignature(domain, signingKey)
          else InvalidSignature
      })
    }.getOrElse(Future(NoSignaturePresent))
  }

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

  private def baseSigningPayload(domain: String): ByteString = {
    val baseUrl = s"${this.copy(signature = None).toUrl}&origin_domain=${URLEncoder.encode(domain, "UTF-8")}"
    new Buffer()
      .write(Array.fill(35)(0.toByte))
      .writeByte(4)
      .write("stellar.sep.7 - URI Scheme".getBytes("UTF-8"))
      .write(baseUrl.getBytes("UTF-8"))
      .readByteString()
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

    //noinspection DuplicatedCode
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

case class DomainSignature(
  originDomain: String,
  signature: ByteString
)

sealed trait SignatureValidation

case object NoSignaturePresent extends SignatureValidation

case class ValidSignature(
  originDomain: String,
  signedBy: PublicKey
) extends SignatureValidation

case object InvalidSignature extends SignatureValidation
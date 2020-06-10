package stellar.sdk.model

import java.net.{URI, URLDecoder, URLEncoder}

import okhttp3.HttpUrl
import stellar.sdk.PublicNetwork

/** A request to a transaction to be signed.
 *
 * @see See [[https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0007.md#operation-tx SEP-0007 for full specification]]
 */
case class TransactionSigningRequest(
  transaction: SignedTransaction
) {

  val toURL: String = s"web+stellar:tx?xdr=${URLEncoder.encode(transaction.encodeXDR, "UTF-8")}"

}

object TransactionSigningRequest {

  def apply(url: String): TransactionSigningRequest = {
    require(url.startsWith("web+stellar:tx?"))
    val httpUrl = HttpUrl.parse(url.replaceFirst("web\\+stellar:", "http://a/"))
    Option(httpUrl.queryParameter("xdr"))
        .map(SignedTransaction.decodeXDR(_)(PublicNetwork))
        .map(TransactionSigningRequest(_))
      .getOrElse(throw new IllegalArgumentException(s"Invalid url: [url=$url]"))
  }

}
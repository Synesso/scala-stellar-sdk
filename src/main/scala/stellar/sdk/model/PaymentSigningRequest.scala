package stellar.sdk.model

import okhttp3.HttpUrl
import stellar.sdk.{KeyPair, PublicKey}

/**
 * A request for a payment to be signed.
 *
 * @param destination A valid account ID for the payment
 */
case class PaymentSigningRequest(
  destination: PublicKey
) {

  def toUrl: String = {
    val params = Map(
      "destination" -> destination.accountId
    )

    params.foldLeft(new HttpUrl.Builder()
      .scheme("http")
      .host("z")
      .addPathSegment("pay")) { case (builder, (key, value)) =>
      builder.addQueryParameter(key, value)
    }.toString()
      .replaceFirst("http://z/", "web+stellar:")
  }
}

object PaymentSigningRequest {
  def apply(url: String): PaymentSigningRequest = {
    require(url.startsWith("web+stellar:pay?"))
    val httpUrl = HttpUrl.parse(url.replaceFirst("web\\+stellar:", "http://a/"))
    Option(httpUrl.queryParameter("destination")).map(KeyPair.fromAccountId).map { destination =>
      PaymentSigningRequest(
        destination = destination
      )
    }
    .getOrElse(throw new IllegalArgumentException(s"Invalid url: [url=$url]"))
  }
}
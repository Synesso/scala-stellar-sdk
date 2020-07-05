package stellar.sdk.model

import okhttp3.HttpUrl
import stellar.sdk.{KeyPair, PublicKey}

/**
 * A request for a payment to be signed.
 *
 * @param destination A valid account ID for the payment
 * @param amount optionally, a specific amount to pay
 */
case class PaymentSigningRequest(
  destination: PublicKey,
  amount: Option[Amount]
) {

  def toUrl: String = {
    val params = Map(
      "destination" -> Some(destination.accountId),
      "amount" -> amount.map(_.units.toString),
      "asset_code" -> amount.filterNot(_.asset == NativeAsset).map(_.asset.code),
      "asset_issuer" -> amount.filterNot(_.asset == NativeAsset).map(_.asset.asInstanceOf[NonNativeAsset].issuer.accountId)
    ).filter(_._2.isDefined).foldLeft(Map.empty[String, String]) {
      case (m, (k, Some(v))) => m.updated(k, v)
      case (m, _) =>            m
    }

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

    val amount = Option(httpUrl.queryParameter("amount")).map(_.toLong).flatMap { units =>
      (for {
        code <- Option(httpUrl.queryParameter("asset_code"))
        issuer <- Option(httpUrl.queryParameter("asset_issuer")).map(KeyPair.fromAccountId)
      } yield Asset(code, issuer))
        .map { Amount(units, _) }
        .orElse(Some(NativeAmount(units)))
    }

    Option(httpUrl.queryParameter("destination")).map(KeyPair.fromAccountId).map { destination =>
      PaymentSigningRequest(
        destination = destination,
        amount
      )
    }
    .getOrElse(throw new IllegalArgumentException(s"Invalid url: [url=$url]"))
  }
}
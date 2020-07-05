package stellar.sdk.model

import okhttp3.HttpUrl
import stellar.sdk.util.ByteArrays
import stellar.sdk.{KeyPair, PublicKey}

import scala.Option

/**
 * A request for a payment to be signed.
 *
 * @param destination A valid account ID for the payment
 * @param amount optionally, a specific amount to pay
 * @param memo optionally, a memo to attach to the transaction
 */
case class PaymentSigningRequest(
  destination: PublicKey,
  amount: Option[Amount] = None,
  memo: Memo = NoMemo
) {

  def toUrl: String = {

    val memoEncoded: Option[(String, String)] = memo match {
      case NoMemo => None
      case MemoId(id) => Some(id.toString -> "MEMO_ID")
      case MemoText(text) => Some(text -> "MEMO_TEXT")
      case MemoHash(xs) => Some(ByteArrays.base64(xs) -> "MEMO_HASH")
      case MemoReturnHash(xs) => Some(ByteArrays.base64(xs) -> "MEMO_RETURN")
    }

    val params = Map(
      "destination" -> Some(destination.accountId),
      "amount" -> amount.map(_.units.toString),
      "asset_code" -> amount.filterNot(_.asset == NativeAsset).map(_.asset.code),
      "asset_issuer" -> amount.filterNot(_.asset == NativeAsset).map(_.asset.asInstanceOf[NonNativeAsset].issuer.accountId),
      "memo" -> memoEncoded.map(_._1),
      "memo_type" -> memoEncoded.map(_._2)
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

    val memo = (for {
      memoValue <- Option(httpUrl.queryParameter("memo"))
      memoType <- Option(httpUrl.queryParameter("memo_type"))
    } yield {
      memoType match {
        case "MEMO_ID" => MemoId(memoValue.toLong)
        case "MEMO_TEXT" => MemoText(memoValue)
        case "MEMO_HASH" => MemoHash(ByteArrays.base64(memoValue))
        case "MEMO_RETURN" => MemoReturnHash(ByteArrays.base64(memoValue))
      }
    }).getOrElse(NoMemo)

    Option(httpUrl.queryParameter("destination")).map(KeyPair.fromAccountId).map { destination =>
      PaymentSigningRequest(destination, amount, memo)
    }
    .getOrElse(throw new IllegalArgumentException(s"Invalid url: [url=$url]"))
  }
}
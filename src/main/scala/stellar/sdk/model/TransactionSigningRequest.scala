package stellar.sdk.model

import okhttp3.HttpUrl
import stellar.sdk.PublicNetwork

/** A request to a transaction to be signed.
 *
 * @param transaction The signed transaction to be encoded
 * @param form The additional information required by the user in the form `form_label -> (txrep_field, form_hint)`
 * @see See [[https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0007.md#operation-tx SEP-0007 for full specification]]
 */
case class TransactionSigningRequest(
  transaction: SignedTransaction,
  form: Map[String, (String, String)] = Map.empty
) {
  form.keys.foreach(validateFormLabel)

  def toUrl: String = {
    val encodedForm = if (form.isEmpty) None else {
      val fieldToLabel = form.map { case (label, (txRepField, _)) => s"$txRepField:$label" }.mkString(",")
      val labelToHint = form.map { case (label, (_, hint)) => s"$label:$hint" }.mkString(",")
      Some("replace" -> s"$fieldToLabel;$labelToHint")
    }

    val params = List(encodedForm).flatten.foldLeft(Map("xdr" -> transaction.encodeXDR)) { case (map, (key, value)) =>
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

    Option(httpUrl.queryParameter("xdr"))
        .map(SignedTransaction.decodeXDR(_)(PublicNetwork))
        .map(TransactionSigningRequest(_, form))
      .getOrElse(throw new IllegalArgumentException(s"Invalid url: [url=$url]"))
  }

  private def splitOnce(str: String, sep: String, reverse: Boolean = false): (String, String) = {
    val split: List[String] =
      if (reverse) str.reverse.split(sep).toList.map(_.reverse)
      else str.split(sep).toList

    split match {
      case h +: ts => h -> ts.mkString(sep)
//      case Nil => sep -> "" // special case where a single value is equal to the separator
      case _ => throw new IllegalArgumentException(s"Cannot split by separator [str=$str][sep=$sep]")
    }
  }

}
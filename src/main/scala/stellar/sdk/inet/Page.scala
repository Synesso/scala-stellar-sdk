package stellar.sdk.inet

import com.softwaremill.sttp.Uri

/**
  * A page of results
  */
case class Page[T](xs: Seq[T], self: Uri, next: Uri)

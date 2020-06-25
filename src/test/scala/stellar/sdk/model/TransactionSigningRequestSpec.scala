package stellar.sdk.model

import java.net.URLEncoder

import okhttp3.HttpUrl
import org.scalacheck.Gen
import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers}

import scala.annotation.tailrec

class TransactionSigningRequestSpec extends Specification with ArbitraryInput with DomainMatchers {

  "encoding as a web+stellar url" should {
    "decode to the original" >> prop { signingRequest: TransactionSigningRequest =>
      TransactionSigningRequest(signingRequest.toUrl) must beEquivalentTo(signingRequest)
    }
  }

  "constructing with form request" should {
    "fail when form field is blank" >> prop { signedTransaction: SignedTransaction =>
      TransactionSigningRequest(signedTransaction, Map("" -> ("", ""))) must throwAn[IllegalArgumentException]
    }.set(minTestsOk = 1)

    "fail when form field contains a colon" >> prop { signedTransaction: SignedTransaction =>
      TransactionSigningRequest(signedTransaction, Map("abc:123" -> ("", ""))) must throwAn[IllegalArgumentException]
    }.set(minTestsOk = 1)
  }

  "parsing from url" should {
    "fail when xdr param is missing" >> {
      TransactionSigningRequest("web+stellar:tx?foo=bar") must throwAn[IllegalArgumentException]
    }

    "fail when replace param has mismatched field names" >> prop { signedTransaction: SignedTransaction =>
      val txn = URLEncoder.encode(signedTransaction.encodeXDR, "UTF-8")
      val replace = URLEncoder.encode("tx.sourceAccount:foo;bar:The source account", "UTF-8")
      TransactionSigningRequest(s"web+stellar:tx?xdr=$txn&replace=$replace") must throwAn[IllegalArgumentException]
    }.set(minTestsOk = 1)

    "fail when callback is not a url" >> {
      val request: String = sampleOne(genTransactionSigningRequest).copy(callback = None).toUrl
      TransactionSigningRequest(s"$request&callback=nonsense") must throwAn[IllegalArgumentException]
    }
  }

  @tailrec
  private def sampleOne[T](genT: Gen[T]): T = genT.sample match {
    case None => sampleOne(genT)
    case Some(t) => t
  }
}

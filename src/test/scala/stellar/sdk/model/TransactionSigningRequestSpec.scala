package stellar.sdk.model

import java.net.URLEncoder

import okhttp3.HttpUrl
import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.scalacheck.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.{ArbitraryInput, DomainMatchers, KeyPair}
import scala.concurrent.duration._

import scala.annotation.tailrec

class TransactionSigningRequestSpec(implicit ee: ExecutionEnv) extends Specification with ArbitraryInput with DomainMatchers {

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

  "constructing with callback url" should {
    "prepend `url:` to the value" >> {
      val request: String = sampleOne(genTransactionSigningRequest)
        .copy(callback = Some(HttpUrl.parse("https://google.com/")))
        .toUrl
      request must contain("&callback=url%3Ahttps%3A%2F%2Fgoogle.com%2F")
    }
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
      TransactionSigningRequest(s"$request&callback=url%3Anonsense") must throwAn[IllegalArgumentException]
    }

    "fail when msg is greater than 300 chars" >> {
      val request: String = sampleOne(genTransactionSigningRequest).copy(message = None).toUrl
      TransactionSigningRequest(s"$request&msg=${"x" * 301}") must throwAn[IllegalArgumentException]
    }
  }

  "signing a request" should {
    "add a valid signature" >> {
      val signingRequest: TransactionSigningRequest = sampleOne(genTransactionSigningRequest)
      val requestSigner = KeyPair.random
      val server = new MockWebServer
      server.enqueue(new MockResponse().setBody(s"""URI_REQUEST_SIGNING_KEY="${requestSigner.accountId}""""))
      server.start()
      val signedRequest = signingRequest.sign(server.getHostName, requestSigner)
      signedRequest.signature must beSome[DomainSignature]
      val hasValidSignature = signedRequest.validateSignature(useHttps = false, port = server.getPort())
      hasValidSignature.onComplete(_ => server.shutdown())
      hasValidSignature must beLikeA[SignatureValidation] { case ValidSignature(domain, publicKey) =>
        domain mustEqual server.getHostName
        publicKey.accountId mustEqual requestSigner.accountId
      }.await
    }

    "fail signature validation if there is no signature" >> {
      val signingRequest: TransactionSigningRequest = sampleOne(genTransactionSigningRequest)
      val requestSigner = KeyPair.random
      val hasValidSignature = signingRequest.validateSignature()
      hasValidSignature must beEqualTo(NoSignaturePresent).await
    }

    "fail signature validation if the host doesn't exist" >> {
      val signingRequest: TransactionSigningRequest = sampleOne(genTransactionSigningRequest)
      val requestSigner = KeyPair.random
      val signedRequest = signingRequest.sign("skajdfhalksjdfhalksf.com", requestSigner)
      val hasValidSignature = signedRequest.validateSignature()
      hasValidSignature must beEqualTo(InvalidSignature).await(0, 30.seconds)
    }

    "fail signature validation if the host doesn't have domain info" >> {
      val signingRequest: TransactionSigningRequest = sampleOne(genTransactionSigningRequest)
      val requestSigner = KeyPair.random
      val signedRequest = signingRequest.sign("example.com", requestSigner)
      val hasValidSignature = signedRequest.validateSignature()
      hasValidSignature must beEqualTo(InvalidSignature).await(0, 30.seconds)
    }

    "fail signature validation if the domain info doesn't have a declared signer" >> {
      val signingRequest: TransactionSigningRequest = sampleOne(genTransactionSigningRequest)
      val requestSigner = KeyPair.random
      val server = new MockWebServer
      server.enqueue(new MockResponse().setBody(""))
      server.start()
      val signedRequest = signingRequest.sign(server.getHostName, requestSigner)
      signedRequest.signature must beSome[DomainSignature]
      val hasValidSignature = signedRequest.validateSignature(useHttps = false, port = server.getPort())
      hasValidSignature.onComplete(_ => server.shutdown())
      hasValidSignature must beEqualTo(InvalidSignature).await(0, 30.seconds)
    }

    "fail signature validation if the signature byes do not match the declared signer" >> {
      val signingRequest: TransactionSigningRequest = sampleOne(genTransactionSigningRequest)
      val requestSigner = KeyPair.random
      val server = new MockWebServer
      server.enqueue(new MockResponse().setBody(s"""URI_REQUEST_SIGNING_KEY="${KeyPair.random.accountId}""""))
      server.start()
      val signedRequest = signingRequest.sign(server.getHostName, requestSigner)
      signedRequest.signature must beSome[DomainSignature]
      val hasValidSignature = signedRequest.validateSignature(useHttps = false, port = server.getPort())
      hasValidSignature.onComplete(_ => server.shutdown())
      hasValidSignature must beEqualTo(InvalidSignature).await(0, 30.seconds)
    }
  }
}

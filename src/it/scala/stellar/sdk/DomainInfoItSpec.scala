package stellar.sdk

import okhttp3.HttpUrl
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.model.domain.{DomainInfo, IssuerDocumentation, Validator}

import scala.concurrent.duration._

class DomainInfoItSpec(implicit ee: ExecutionEnv) extends Specification {

  "known stellar.toml files" should {
    "parse correctly" >> {
      // #domain_info_example
      DomainInfo.forDomain("stellar.org") must beSome(
        DomainInfo(
          issuerDocumentation = Some(IssuerDocumentation(
            name = Some("Stellar Development Foundation"),
            url = Some(HttpUrl.parse("https://www.stellar.org")),
            github = Some("stellar"),
            twitter = Some("StellarOrg"),
          )),
          validators = List(
            Validator(
              alias = Some("sdf1"),
              displayName = Some("SDF 1"),
              host = Some("core-live-a.stellar.org:11625"),
              publicKey = Some(KeyPair.fromAccountId("GCGB2S2KGYARPVIA37HYZXVRM2YZUEXA6S33ZU5BUDC6THSB62LZSTYH")),
              history = Some(HttpUrl.parse("http://history.stellar.org/prd/core-live/core_live_001/"))
            ),
            Validator(
              alias = Some("sdf2"),
              displayName = Some("SDF 2"),
              host = Some("core-live-b.stellar.org:11625"),
              publicKey = Some(KeyPair.fromAccountId("GCM6QMP3DLRPTAZW2UZPCPX2LF3SXWXKPMP3GKFZBDSF3QZGV2G5QSTK")),
              history = Some(HttpUrl.parse("http://history.stellar.org/prd/core-live/core_live_002/"))
            ),
            Validator(
              alias = Some("sdf3"),
              displayName = Some("SDF 3"),
              host = Some("core-live-c.stellar.org:11625"),
              publicKey = Some(KeyPair.fromAccountId("GABMKJM6I25XI4K7U6XWMULOUQIQ27BCTMLS6BYYSOWKTBUXVRJSXHYQ")),
              history = Some(HttpUrl.parse("http://history.stellar.org/prd/core-live/core_live_003/"))
            ),
          )
        )
      )
      // #domain_info_example
      .awaitFor(30.seconds)
    }
  }
}

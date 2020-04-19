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
          version = Some("2.0.0"),
          accounts = List(
            KeyPair.fromAccountId("GB6NVEN5HSUBKMYCE5ZOWSK5K23TBWRUQLZY3KNMXUZ3AQ2ESC4MY4AQ"),
            KeyPair.fromAccountId("GATL3ETTZ3XDGFXX2ELPIKCZL7S5D2HY3VK4T7LRPD6DW5JOLAEZSZBA"),
            KeyPair.fromAccountId("GCVLWV5B3L3YE6DSCCMHLCK7QIB365NYOLQLW3ZKHI5XINNMRLJ6YHVX"),
            KeyPair.fromAccountId("GCVJDBALC2RQFLD2HYGQGWNFZBCOD2CPOTN3LE7FWRZ44H2WRAVZLFCU"),
            KeyPair.fromAccountId("GAMGGUQKKJ637ILVDOSCT5X7HYSZDUPGXSUW67B2UKMG2HEN5TPWN3LQ"),
            KeyPair.fromAccountId("GDUY7J7A33TQWOSOQGDO776GGLM3UQERL4J3SPT56F6YS4ID7MLDERI4"),
            KeyPair.fromAccountId("GCPWKVQNLDPD4RNP5CAXME4BEDTKSSYRR4MMEL4KG65NEGCOGNJW7QI2"),
            KeyPair.fromAccountId("GDKIJJIKXLOM2NRMPNQZUUYK24ZPVFC6426GZAEP3KUK6KEJLACCWNMX"),
            KeyPair.fromAccountId("GAX3BRBNB5WTJ2GNEFFH7A4CZKT2FORYABDDBZR5FIIT3P7FLS2EFOZZ"),
            KeyPair.fromAccountId("GBEVKAYIPWC5AQT6D4N7FC3XGKRRBMPCAMTO3QZWMHHACLHTMAHAM2TP"),
            KeyPair.fromAccountId("GCKJZ2YVECFGLUDJ5T7NZMJPPWERBNYHCXT2MZPXKELFHUSYQR5TVHJQ"),
            KeyPair.fromAccountId("GBA6XT7YBQOERXT656T74LYUVJ6MEIOC5EUETGAQNHQHEPUFPKCW5GYM"),
            KeyPair.fromAccountId("GD2D6JG6D3V52ZMPIYSVHYFKVNIMXGYVLYJQ3HYHG5YDPGJ3DCRGPLTP"),
            KeyPair.fromAccountId("GA2VRL65L3ZFEDDJ357RGI3MAOKPJZ2Z3IJTPSC24I4KDTNFSVEQURRA")
          ),
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

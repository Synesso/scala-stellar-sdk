package stellar.sdk.model

import org.json4s.NoTypeHints
import org.json4s.native.{JsonMethods, Serialization}
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput

class PaymentPathSpec extends Specification with ArbitraryInput {

  implicit val formats = Serialization.formats(NoTypeHints) + PaymentPathDeserializer

  "a payment path response documents" should {
    "parse to a payment path" >> prop { path: PaymentPath =>

      def amountJson(prefix: String, amount: Amount) =
        s"""
           |"${prefix}amount": "${amount.toDisplayUnits}",
           |${assetJson(prefix, amount.asset)}
         """.stripMargin

      def assetJson(prefix: String, asset: Asset) = {
        asset match {
          case NativeAsset => s""""${prefix}asset_type": "native""""
          case issuedAsset: NonNativeAsset =>
            s"""
               |"${prefix}asset_type": "${issuedAsset.typeString}",
               |"${prefix}asset_code": "${issuedAsset.code}",
               |"${prefix}asset_issuer": "${issuedAsset.issuer.accountId}"
            """.stripMargin
        }
      }

      val json =
        s"""
           |{
           |  ${amountJson("source_", path.source)},
           |  ${amountJson("destination_", path.destination)},
           |  "path": ${path.path.map(j => s"{${assetJson("", j)}}").mkString("[", ",", "]")}
           |}
         """.stripMargin

      JsonMethods.parse(json).extract[PaymentPath] mustEqual path
    }
  }

  "the underlying amount parser" should {
    "not parse unrecognised asset type" >> {
      val doc = """{"foo_asset_type":"bananas"}"""
      AmountParser.parseAsset("foo_", JsonMethods.parse(doc)) must throwA[RuntimeException]
    }
  }

}

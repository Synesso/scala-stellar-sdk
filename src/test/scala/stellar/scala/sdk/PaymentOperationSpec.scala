package stellar.scala.sdk

import org.specs2.mutable.Specification

class PaymentOperationSpec extends Specification with ArbitraryInput {

  "payment operation" should {
    "serde via xdr" >> prop { (source: KeyPair, destination: VerifyingKey, amount: Amount, asset: Asset) =>
      val input = PaymentOperation(source, destination, asset, amount)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case pa: PaymentOperation =>
          pa.destinationAccount.accountId mustEqual destination.accountId
          pa.amount mustEqual amount
          pa.asset must beLike {
            case AssetTypeNative => asset mustEqual AssetTypeNative
            case AssetTypeCreditAlphaNum4(code, issuer) =>
              val AssetTypeCreditAlphaNum4(expectedCode, expectedIssuer) = asset
              code mustEqual expectedCode
              issuer.accountId mustEqual expectedIssuer.accountId
            case AssetTypeCreditAlphaNum12(code, issuer) =>
              val AssetTypeCreditAlphaNum12(expectedCode, expectedIssuer) = asset
              code mustEqual expectedCode
              issuer.accountId mustEqual expectedIssuer.accountId
          }
          pa.sourceAccount must beNone
      }
    }
  }

}

package stellar.scala.sdk

import org.specs2.mutable.Specification

class PaymentOperationSpec extends Specification with ArbitraryInput {

  "payment operation" should {
    "serde via xdr" >> prop { (source: KeyPair, destination: VerifyingKey, amount: Amount) =>
      val input = PaymentOperation(source, destination, amount)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case pa: PaymentOperation =>
          pa.destinationAccount.accountId mustEqual destination.accountId
          pa.amount.units mustEqual amount.units
          pa.amount.asset must beLike {
            case AssetTypeNative => amount.asset mustEqual AssetTypeNative
            case AssetTypeCreditAlphaNum4(code, issuer) =>
              val AssetTypeCreditAlphaNum4(expectedCode, expectedIssuer) = amount.asset
              code mustEqual expectedCode
              issuer.accountId mustEqual expectedIssuer.accountId
            case AssetTypeCreditAlphaNum12(code, issuer) =>
              val AssetTypeCreditAlphaNum12(expectedCode, expectedIssuer) = amount.asset
              code mustEqual expectedCode
              issuer.accountId mustEqual expectedIssuer.accountId
          }
          pa.sourceAccount must beNone
      }
    }
  }

}

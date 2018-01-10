package stellar.scala.sdk

import org.specs2.mutable.Specification

class CreateAccountOperationSpec extends Specification with ArbitraryInput {

  "create account operation" should {
    "serde via xdr" >> prop { (source: KeyPair, destination: VerifyingKey, amount: NativeAmount) =>
      val input = CreateAccountOperation(source, destination, amount)
      Operation.fromXDR(input.toXDR) must beSuccessfulTry.like {
        case ca: CreateAccountOperation =>
          ca.destinationAccount.accountId mustEqual destination.accountId
          ca.startingBalance.units mustEqual amount.units
          ca.startingBalance.asset must beLike {
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
          ca.sourceAccount must beNone
      }
    }
  }

}

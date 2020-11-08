package stellar.sdk.model

import okio.ByteString
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.model.ClaimableBalanceGenerators.genClaimableBalance
import stellar.sdk.model.ClaimantGenerators.genClaimant

class ClaimableBalanceSpec extends Specification with ScalaCheck {

  implicit val arbClaimableBalance: Arbitrary[ClaimableBalance] = Arbitrary(genClaimableBalance)
  implicit val formats: Formats = DefaultFormats + ClaimableBalanceDeserializer

  "a claimable balance" should {
    "decode from JSON" >> prop { balance: ClaimableBalance =>
      JsonMethods.parse(ClaimableBalanceGenerators.json(balance)).extract[ClaimableBalance] mustEqual balance
    }
  }

}

object ClaimableBalanceGenerators extends ArbitraryInput {

  def genClaimableBalance: Gen[ClaimableBalance] = for {
    id <- Gen.containerOfN[Array, Byte](32, Gen.posNum[Byte]).map(new ByteString(_)).map(ClaimableBalanceHashId)
    amount <- genAmount
    sponsor <- genPublicKey
    claimants <- Gen.nonEmptyListOf[Claimant](genClaimant)
    lastModifiedLedger <- Gen.posNum[Long]
    lastModifiedTime <- genInstant
  } yield ClaimableBalance(id, amount, sponsor, claimants, lastModifiedLedger, lastModifiedTime)

  def json(balance: ClaimableBalance): String =
    s"""{
       |  "id": "${balance.id.encodeString}",
       |  "asset": "${balance.amount.asset.canoncialString}",
       |  "amount": "${balance.amount.toDisplayUnits}",
       |  "sponsor": "${balance.sponsor.accountId}",
       |  "last_modified_ledger": ${balance.lastModifiedLedger},
       |  "last_modified_time": "${balance.lastModifiedTime}",
       |  "claimants": [${balance.claimants.map(ClaimantGenerators.json).mkString(",")}]
       |}""".stripMargin
}

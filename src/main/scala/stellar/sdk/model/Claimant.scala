package stellar.sdk.model

import org.json4s.JsonAST.JObject
import org.json4s.{DefaultFormats, Formats}
import org.stellar.xdr.Claimant.ClaimantV0
import org.stellar.xdr.{ClaimantType, Claimant => XClaimant}
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.{KeyPair, PublicKeyOps}

sealed trait Claimant {
  def xdr: XClaimant
}

object Claimant {

  def decodeXdr(xdr: XClaimant): Claimant =
    xdr.getDiscriminant match {
      case ClaimantType.CLAIMANT_TYPE_V0 =>
        AccountIdClaimant(
          accountId = AccountId.decodeXdr(xdr.getV0.getDestination).publicKey,
          predicate = ClaimPredicate.decodeXdr(xdr.getV0.getPredicate)
        )
    }
}

case class AccountIdClaimant(
  accountId: PublicKeyOps,
  predicate: ClaimPredicate
) extends Claimant {
  def xdr: XClaimant = new XClaimant.Builder()
    .discriminant(ClaimantType.CLAIMANT_TYPE_V0)
    .v0(new ClaimantV0.Builder()
      .destination(accountId.toAccountId.xdr)
      .predicate(predicate.xdr)
      .build())
    .build()
}

object ClaimantDeserializer extends ResponseParser[Claimant]({ o: JObject =>
  implicit val formats: Formats = DefaultFormats + ClaimPredicateDeserializer

  AccountIdClaimant(
    accountId = KeyPair.fromAccountId((o \ "destination").extract[String]),
    predicate = (o \ "predicate").extract[ClaimPredicate]
  )
})
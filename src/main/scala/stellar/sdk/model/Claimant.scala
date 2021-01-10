package stellar.sdk.model

import org.json4s.{DefaultFormats, Formats}
import org.json4s.JsonAST.JObject
import org.stellar.xdr
import org.stellar.xdr.{Claimant => XClaimant}
import org.stellar.xdr.Claimant.ClaimantV0
import org.stellar.xdr.ClaimantType
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.{KeyPair, PublicKeyOps}

sealed trait Claimant {
  def xdr: XClaimant
}

object Claimant {
  def decode(xdr: XClaimant): Claimant =
    xdr.getDiscriminant match {
      case ClaimantType.CLAIMANT_TYPE_V0 =>
        AccountIdClaimant(
          accountId = AccountId.decode(xdr.getV0.getDestination).publicKey,
          predicate = ClaimPredicate.decode(xdr.getV0.getPredicate)
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
      .destination(accountId.toAccountId.accountIdXdr)
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
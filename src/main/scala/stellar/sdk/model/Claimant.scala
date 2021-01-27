package stellar.sdk.model

import cats.data.State
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import org.stellar.xdr.Claimant.ClaimantV0
import org.stellar.xdr.{ClaimantType, Claimant => XClaimant}
import stellar.sdk.model.response.ResponseParser
import stellar.sdk.model.xdr.Encode.int
import stellar.sdk.model.xdr.{Decode, Encodable}
import stellar.sdk.{KeyPair, PublicKeyOps}

sealed trait Claimant extends Encodable {
  def xdr: XClaimant
}

object Claimant extends Decode {

  def decodeXdr(xdr: XClaimant): Claimant =
    xdr.getDiscriminant match {
      case ClaimantType.CLAIMANT_TYPE_V0 =>
        AccountIdClaimant(
          accountId = AccountId.decodeXdr(xdr.getV0.getDestination).publicKey,
          predicate = ClaimPredicate.decodeXdr(xdr.getV0.getPredicate)
        )
    }

  val decode: State[Seq[Byte], Claimant] = for {
    _ <- int
    accountId <- KeyPair.decode
    predicate <- ClaimPredicate.decode
  } yield AccountIdClaimant(accountId, predicate)
}

case class AccountIdClaimant(
  accountId: PublicKeyOps,
  predicate: ClaimPredicate
) extends Claimant {
  override def encode: LazyList[Byte] = int(0) ++ accountId.encode ++ predicate.encode

  def xdr: XClaimant = new XClaimant.Builder()
    .discriminant(ClaimantType.CLAIMANT_TYPE_V0)
    .v0(new ClaimantV0.Builder()
      .destination(accountId.toAccountId.xdr)
      .predicate(predicate.xdr)
      .build())
    .build()
}

object ClaimantDeserializer extends ResponseParser[Claimant]({ o: JObject =>
  implicit val formats = DefaultFormats + ClaimPredicateDeserializer

  AccountIdClaimant(
    accountId = KeyPair.fromAccountId((o \ "destination").extract[String]),
    predicate = (o \ "predicate").extract[ClaimPredicate]
  )
})
package stellar.sdk.model.op

import cats.data.State
import stellar.sdk.{KeyPair, PublicKeyOps}
import stellar.sdk.model.Signer
import stellar.sdk.model.xdr.{Decode, Encode}

/**
  * Modify an account, setting one or more options.
  *
  * @param inflationDestination the account's inflation destination
  * @param clearFlags issuer flags to be turned off
  * @param setFlags issuer flags to be turned on
  * @param masterKeyWeight the weight of the master key
  * @param lowThreshold the minimum weight required for low threshold operations
  * @param mediumThreshold the minimum weight required for medium threshold operations
  * @param highThreshold the minimum weight required for highthreshold operations
  * @param homeDomain the home domain used for reverse federation lookup
  * @param signer the key and weight of the signer for this account
  * @param sourceAccount the account effecting this operation, if different from the owning account of the transaction
  * @see [[https://www.stellar.org/developers/horizon/reference/resources/operation.html#set-options endpoint doc]]
  */
case class SetOptionsOperation(inflationDestination: Option[PublicKeyOps] = None,
                               clearFlags: Option[Set[IssuerFlag]] = None,
                               setFlags: Option[Set[IssuerFlag]] = None,
                               masterKeyWeight: Option[Int] = None,
                               lowThreshold: Option[Int] = None,
                               mediumThreshold: Option[Int] = None,
                               highThreshold: Option[Int] = None,
                               homeDomain: Option[String] = None,
                               signer: Option[Signer] = None,
                               sourceAccount: Option[PublicKeyOps] = None) extends Operation {

  override def encode: Stream[Byte] =
    super.encode ++
      Encode.int(5) ++
      Encode.opt(inflationDestination) ++
      Encode.optInt(clearFlags.map(_.map(_.i) + 0).map(_.reduce(_ | _))) ++
      Encode.optInt(setFlags.map(_.map(_.i) + 0).map(_.reduce(_ | _))) ++
      Encode.optInt(masterKeyWeight) ++
      Encode.optInt(lowThreshold) ++
      Encode.optInt(mediumThreshold) ++
      Encode.optInt(highThreshold) ++
      Encode.optString(homeDomain) ++
      Encode.opt(signer)
}

object SetOptionsOperation {

  def decode: State[Seq[Byte], SetOptionsOperation] = for {
    inflationDestination <- Decode.opt(KeyPair.decode)
    clearFlags <- Decode.opt(Decode.int.map(IssuerFlags.from))
    setFlags <- Decode.opt(Decode.int.map(IssuerFlags.from))
    masterKeyWeight <- Decode.opt(Decode.int)
    lowThreshold <- Decode.opt(Decode.int)
    mediumThreshold <- Decode.opt(Decode.int)
    highThreshold <- Decode.opt(Decode.int)
    homeDomain <- Decode.opt(Decode.string)
    signer <- Decode.opt(Signer.decode)
  } yield SetOptionsOperation(inflationDestination, clearFlags, setFlags, masterKeyWeight, lowThreshold,
      mediumThreshold, highThreshold, homeDomain, signer)

}

sealed trait IssuerFlag {
  val i: Int
  val s: String
}

case object AuthorizationRequiredFlag extends IssuerFlag {
  val i = 0x1
  val s = "auth_required_flag"
}

case object AuthorizationRevocableFlag extends IssuerFlag {
  val i = 0x2
  val s = "auth_revocable_flag"
}

case object AuthorizationImmutableFlag extends IssuerFlag {
  val i = 0x4
  val s = "auth_immutable_flag"
}

object IssuerFlags {
  val all: Set[IssuerFlag] = Set(AuthorizationRequiredFlag, AuthorizationRevocableFlag, AuthorizationImmutableFlag)

  def apply(i: Int): Option[IssuerFlag] = all.find(_.i == i)

  def from(i: Int): Set[IssuerFlag] = all.filter { f => (i & f.i) == f.i }
}

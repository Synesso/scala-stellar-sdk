package stellar.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.SET_OPTIONS
import org.stellar.sdk.xdr.{Signer => XDRSigner, _}
import stellar.sdk.XDRPrimitives._
import stellar.sdk._

import scala.util.Try

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

  override def toOperationBody: OperationBody = {
    val op = new SetOptionsOp
    inflationDestination.foreach { pk =>
      op.setInflationDest(new AccountID)
      op.getInflationDest.setAccountID(pk.getXDRPublicKey)
    }
    clearFlags.map(_.map(_.i) + 0).map(_.reduce(_ | _)).map(uint32).foreach(op.setClearFlags)
    setFlags.map(_.map(_.i) + 0).map(_.reduce(_ | _)).map(uint32).foreach(op.setSetFlags)
    masterKeyWeight.map(uint32).foreach(op.setMasterWeight)
    lowThreshold.map(uint32).foreach(op.setLowThreshold)
    mediumThreshold.map(uint32).foreach(op.setMedThreshold)
    highThreshold.map(uint32).foreach(op.setHighThreshold)
    homeDomain.map(str32).foreach(op.setHomeDomain)
    signer.foreach {
      // todo - use https://github.com/stellar/java-stellar-sdk/blob/master/src/main/java/org/stellar/sdk/Signer.java#L19-L73 to complete this
      case AccountSigner(k, w) =>
        val s = new XDRSigner
        s.setKey(k.getXDRSignerKey)
        s.setWeight(uint32(w))
        op.setSigner(s)
      case _ => ???
    }

    val body = new OperationBody
    body.setDiscriminant(SET_OPTIONS)
    body.setSetOptionsOp(op)
    body
  }

}

object SetOptionsOperation {

  def from(op: SetOptionsOp): Try[SetOptionsOperation] = Try {
    SetOptionsOperation(
      clearFlags = Option(op.getClearFlags).map(IssuerFlags.from),
      setFlags = Option(op.getSetFlags).map(IssuerFlags.from),
      inflationDestination = Option(op.getInflationDest).map(dest => KeyPair.fromXDRPublicKey(dest.getAccountID)),
      masterKeyWeight = Option(op.getMasterWeight).map(_.getUint32),
      lowThreshold = Option(op.getLowThreshold).map(_.getUint32),
      mediumThreshold = Option(op.getMedThreshold).map(_.getUint32),
      highThreshold = Option(op.getHighThreshold).map(_.getUint32),
      homeDomain = Option(op.getHomeDomain).map(_.getString32),
      signer = Option(op.getSigner).map(s => AccountSigner(KeyPair.fromPublicKey(s.getKey.getEd25519.getUint256), s.getWeight.getUint32))
    )
  }

}

/*
    val signerKey = new SignerKey
    signerKey.setDiscriminant(SignerKeyType.SIGNER_KEY_TYPE_ED25519)
    val uint256 = new Uint256
    uint256.setUint256(pk.getAbyte)
    signerKey.setEd25519(uint256)

 */

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

  def from(i: Uint32): Set[IssuerFlag] = all.filter { f => (i.getUint32.toInt & f.i) == f.i }
}

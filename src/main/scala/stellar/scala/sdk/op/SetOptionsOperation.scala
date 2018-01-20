package stellar.scala.sdk.op

import org.stellar.sdk.xdr.Operation.OperationBody
import org.stellar.sdk.xdr.OperationType.SET_OPTIONS
import org.stellar.sdk.xdr._
import stellar.scala.sdk._

import scala.util.Try

case class SetOptionsOperation(inflationDestination: Option[PublicKeyOps] = None,
                               clearFlags: Option[Set[IssuerFlag]] = None,
                               setFlags: Option[Set[IssuerFlag]] = None,
                               masterKeyWeight: Option[Int] = None,
                               lowThreshold: Option[Int] = None,
                               mediumThreshold: Option[Int] = None,
                               highThreshold: Option[Int] = None,
                               homeDomain: Option[String] = None,
                               signer: Option[(SignerKey, Int)] = None,
                               sourceAccount: Option[KeyPair] = None) extends Operation {

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
    signer.foreach { case (key, weight) =>
      val s = new Signer
      s.setKey(key)
      s.setWeight(uint32(weight))
      op.setSigner(s)
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
      signer = Option(op.getSigner).map(s => (s.getKey, s.getWeight.getUint32))
    )
  }

}

sealed trait IssuerFlag {
  val i: Int
}

case object AuthorizationRequiredFlag extends IssuerFlag {
  val i = 0x1
}

case object AuthorizationRevocableFlag extends IssuerFlag {
  val i = 0x2
}

case object AuthorizationImmutableFlag extends IssuerFlag {
  val i = 0x4
}

object IssuerFlags {
  val all: Set[IssuerFlag] = Set(AuthorizationRequiredFlag, AuthorizationRevocableFlag, AuthorizationImmutableFlag)

  def from(i: Uint32): Set[IssuerFlag] = all.filter { f => (i.getUint32.toInt & f.i) == f.i }
}

package stellar.sdk.auth

import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, Formats}
import stellar.sdk.PublicNetwork
import stellar.sdk.model.SignedTransaction
import stellar.sdk.util.DoNothingNetwork

/**
 * An authentication challenge as specified in SEP-0010
 * @param signedTransaction a specially formed transaction that forms the basis of the challenge.
 * @param networkPassphrase the passphrase of the network that the transaction is (and should continue to be) signed for.
 */
case class Challenge(
  signedTransaction: SignedTransaction,
  networkPassphrase: String
) {
  def transaction = signedTransaction.transaction

  def toJson: String = {
    compact(render(
      ("transaction" -> signedTransaction.encodeXDR) ~
        ("network_passphrase" -> signedTransaction.transaction.network.passphrase)
    ))
  }
}

object Challenge {

  implicit val formats: Formats = DefaultFormats
  def apply(json: String): Challenge = {
    val o = parse(json)
    implicit val network = (o \ "network_passphrase").extractOpt[String]
      .map(p => new DoNothingNetwork(p))
        .getOrElse(PublicNetwork)
    Challenge(
      signedTransaction = SignedTransaction.decodeXDR((o \ "transaction").extract[String]),
      networkPassphrase = network.passphrase
    )
  }
}
// creates a trustline

import stellar.sdk._
import stellar.sdk.op._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

// declare that we are going to submit transactions to the test network by bringing it into implicit scope
implicit val network = TestNetwork

// the trustor
val source = KeyPair.fromSecretSeed("SBQT6A2GSA3RE5GPMURKKQ5KIS7OUD42USX57UJVHW4ZCB5R7B6CVD62")

// the issuer
val issuer = KeyPair.fromAccountId("GAQUWIRXODT4OE3YE6L4NF3AYSR5ACEHPINM5S3J2F4XKH7FRZD4NDW2")

val response = for {

  // obtain up-to-date data about our source account
  account <- TestNetwork.account(source)

  txn <- Future.fromTry {

    // create a transaction with the create trust operation and sign it
    Transaction(
      Account(keyPair = source, sequenceNumber = account.lastSequence + 1),
      Seq(
        ChangeTrustOperation(IssuedAmount(1000, IssuedAsset12("PANCAKE", issuer)))
      )
    ).sign(source)
  }

  // and submit it
  response <- txn.submit

} yield {
  response
}

// print the results on completion
response onComplete {

  case Success(resp) =>
    println(s"Account ${source.accountId} trusts PANCAKE from ${issuer.accountId}")

  case Failure(t) =>
    println(s"Failed to trust PANCAKE. $t")

}


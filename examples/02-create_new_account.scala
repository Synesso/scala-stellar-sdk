// creates a new account via a payment from an existing account

import stellar.sdk._
import stellar.sdk.op._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

// declare that we are going to submit transactions to the test network by bringing it into implicit scope
implicit val network = TestNetwork

// the account we will pay from
val source = KeyPair.fromSecretSeed("SBQT6A2GSA3RE5GPMURKKQ5KIS7OUD42USX57UJVHW4ZCB5R7B6CVD62")

// the account to be created
val newAccount = KeyPair.random

val response = for {

  // obtain up-to-date data about our source account
  account <- TestNetwork.account(source)

  txn <- Future.fromTry {

    // create a transaction, add the create account operation and sign it
    Transaction(Account(keyPair = source, sequenceNumber = account.lastSequence + 2))
      .add(CreateAccountOperation(newAccount, Amount.lumens(1)))
      .sign(source)
  }

  // and submit it
  response <- txn.submit

} yield response

// print the results on completion
response onComplete {

  case Success(resp) =>
    println(s"Account ${newAccount.accountId} was created with payment in ledger ${resp.ledger}")

  case Failure(t) =>
    println(s"Failed to create ${newAccount.accountId}. $t")

}

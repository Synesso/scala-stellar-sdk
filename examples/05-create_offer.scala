// creates an offer to trade

import stellar.sdk._
import stellar.sdk.op._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

// declare that we are going to submit transactions to the test network by bringing it into implicit scope
implicit val network = TestNetwork

// the offerer
val source = KeyPair.fromSecretSeed("SBQT6A2GSA3RE5GPMURKKQ5KIS7OUD42USX57UJVHW4ZCB5R7B6CVD62")

// the amount offered
val selling = Amount(100, AssetTypeCreditAlphaNum12("PANCAKE", KeyPair.fromAccountId("GAQUWIRXODT4OE3YE6L4NF3AYSR5ACEHPINM5S3J2F4XKH7FRZD4NDW2")))

// the asset sought
val buying = NativeAsset

// the price offered
val price = Price(n = 5, d = 7)

val response = for {

  // obtain up-to-date data about our source account
  account <- TestNetwork.account(source)

  txn <- Future.fromTry {

    // create a transaction with the create trust operation and sign it
    Transaction(
      Account(keyPair = source, sequenceNumber = account.lastSequence + 1),
      Seq(
        CreateOfferOperation(selling, buying, price)
      )
    ).sign(source)
  }

  // and submit it
  response <- txn.submit

} yield response

// print the results on completion
response onComplete {

  case Success(_) =>
    println(s"Created offer to buy $buying with $selling at $price")

  case Failure(t) =>
    println(s"Failed create offer. $t")

}


[![Travis](https://travis-ci.org/Synesso/scala-stellar-sdk.svg?branch=master)](https://travis-ci.org/Synesso/scala-stellar-sdk)
[![codecov](https://codecov.io/gh/Synesso/scala-stellar-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/Synesso/scala-stellar-sdk)

[![Download](https://api.bintray.com/packages/synesso/mvn/scala-stellar-sdk/images/download.svg)](https://bintray.com/synesso/mvn/scala-stellar-sdk/_latestVersion)

# Stellar SDK for Scala

The Scala SDK for the [Stellar network](https://www.stellar.org/) provides an API to:

* Create accounts
* Build and submit transactions
* Query network state from Horizon
* Stream network updates from Horizon [Pending]
* Query federation servers [Pending]


## Benefits

A Scala developer would choose this SDK over the Java SDK because:

* `Option`s, not nulls
* `Try`s, not exceptions
* `Future`s for all network operations
* Encapsulation of paged responses into `Stream`s
* Case classes instead of builder patterns
* Explicit type hierarchies instead of meaningful primitives
* Test coverage: Generative testing using scalacheck with the goal of 100% coverage
* Perform network operations on the terminal through the scala REPL

## Installation

In your `build.sbt`

```
resolvers += "scala-stellar-sdk-repo" at "https://dl.bintray.com/synesso/mvn"

libraryDependencies +=  "stellar.scala.sdk" %% "scala-stellar-sdk" % "0.1.0"
```

## Examples

The following examples use the [Ammonite REPL](http://ammonite.io/). After launching `amm`, fetch and import the
Stellar SDK for Scala.

```
interp.repositories() ++= Seq(coursier.MavenRepository("https://dl.bintray.com/synesso/mvn/"))

import $ivy.`stellar.scala.sdk::scala-stellar-sdk:0.1.0`
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import stellar.sdk._
import stellar.sdk.op._
import stellar.sdk.resp._
```

### Creating and funding a test account

```
val kp = KeyPair.random
TestNetwork.fund(kp)
```

### Checking the status of an account

```
import scala.util.Success

TestNetwork.account(kp) onSuccess { case resp =>
  println(s"Account ${resp.id}: last sequence was ${resp.lastSequence}. Balances: ${resp.balances.mkString(",")}")
}
```

### Funding a new account with a create account operation

```
implicit val network = TestNetwork

val funder: KeyPair = kp // funded account from first example
val toCreate = KeyPair.random

for {
  account <- TestNetwork.account(funder)
  txn <- Future.fromTry {
    Transaction(Account(funder, account.lastSequence + 1))
      .add(CreateAccountOperation(toCreate, Amount.lumens(1)))
      .sign(funder)
    }
  } yield txn.submit
```

Additional examples can be found in the `/examples` folder.



## Progress

```
[✓] Operations
[✓] Transactions
[ ] Horizon Endpoints
  [✓] Account details
  [✓] Assets
  [✓] Data for account
  [ ] Effects
    [ ] Unfiltered stream
      [✓] Account created
      [✓] Account removed
      [✓] Account credited
      [✓] Account debited
      [✓] Account thresholds updated
      [✓] Account home domain updated
      [ ] Account flags updated - pending https://github.com/stellar/go/issues/294
      [✓] Signer created
      [✓] Signer removed
      [✓] Signer updated
      [✓] Trustline created
      [✓] Trustline removed
      [✓] Trustline updated
      [✓] Trustline authorised
      [✓] Trustline deauthorised
      [ ] Offer created/removed/updated - pending https://github.com/stellar/go/issues/166
      [✓] Trade
    [✓] By account
    [✓] By ledger
    [ ] By operation - pending all operations
    [ ] By transaction - pending all transactions
  [✓] Ledgers
  [✓] Ledger details
  [✓] Offers for account
  [✓] Operations
    [✓] Unfiltered stream
      [✓] Create account
      [✓] Payment
      [✓] Path Payment
      [✓] Manage Offer
      [✓] Create Passive Offer
      [✓] Set Options
      [✓] Allow Trust
      [✓] Change Trust
      [✓] Account Merge
      [✓] Inflation
      [✓] Manage Data
    [✓] By account
    [✓] By ledger
    [✓] By transaction
  [✓] Operation details
  [✓] Orderbook details
  [ ] Payment paths
  [✓] Payments
    [✓] Unfiltered stream
    [✓] By account
    [✓] By ledger
    [] By transaction
  [ ] Trade aggregations
  [ ] Trades
    [ ] Unfiltered
    [ ] By orderbook
  [ ] Transactions
    [ ] Unfiltered
    [ ] By orderbook
    [ ] By account
    [ ] By ledger
  [ ] Post transaction
    [✓] Shallow parse response
    [ ] XDR response parse
    [ ] Type heirarchy for errors
  [ ] Single methods for common operations
    [ ] payment
    [ ] create offer
    [ ] create & fund account
[ ] Streams
  [ ] Effects
  [ ] Ledgers
  [ ] Operations
  [ ] ...
[ ] Federation
```


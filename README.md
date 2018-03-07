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

libraryDependencies +=  "stellar.scala.sdk" %% "scala-stellar-sdk" % "0.0.1.5"
```

## Examples

All of the following examples use the [Ammonite REPL](http://ammonite.io/). After launching `amm`, fetch and import the
Stellar SDK for Scala.

```
interp.repositories() ++= Seq(coursier.MavenRepository("https://dl.bintray.com/synesso/mvn/"))

import $ivy.`stellar.scala.sdk::scala-stellar-sdk:0.0.1.5`
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import stellar.sdk._
import stellar.sdk.resp._
```

### Accounts

#### Creating and funding a test account

```
val kp = KeyPair.random
TestNetwork.fund(kp) // Future[FundTestAccountResp]
```

#### Checking the status of an account

```
TestNetwork.account(kp) // Future[AccountResp]
```

#### Fetch data for an account

```
TestNetwork.accountData(kp, "data_key") // Future[String]
```


### Assets

#### Fetching a stream of all assets

```
TestNetwork.assets // Future[Stream[AssetResp]]
```

#### Filtering assets by code or issuer, or both.

```
TestNetwork.assets(code = Some("ETH"))
TestNetwork.assets(issuer = Some("GAE325UC3T63ROIUFBBRNMWGM7AY2NI5C2YO55IPLRKCF3UECXLXKNNZ"))
TestNetwork.assets(code = Some("ETH"), issuer = Some("GAE325UC3T63ROIUFBBRNMWGM7AY2NI5C2YO55IPLRKCF3UECXLXKNNZ"))
```



## Progress

```
[✓] Operations
[✓] Transactions
[🚀] Horizon Endpoints
  [✓] Account details
  [✓] Assets
    [✓] Unfiltered
    [✓] By code
    [✓] By issuer
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

### todo

* all txns require an account, so account can record the network it came from.

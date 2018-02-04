[![Travis](https://travis-ci.org/Synesso/scala-stellar-sdk.svg?branch=master)](https://travis-ci.org/Synesso/scala-stellar-sdk)
[![codecov](https://codecov.io/gh/Synesso/scala-stellar-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/Synesso/scala-stellar-sdk)

[![Download](https://api.bintray.com/packages/synesso/mvn/scala-stellar-sdk/images/download.svg)](https://bintray.com/synesso/mvn/scala-stellar-sdk/_latestVersion)

# Stellar SDK for Scala

A Scala SDK for the [Stellar network](https://www.stellar.org/). It is a work in progress with a target of being fully functional
by March 15, 2018. Contributions are welcome.

## Progress

[✓] Operations

[✓] Transactions

[🚀] Requests

[ ] Responses

[ ] Effects

[ ] Federation

## Benefits

A Scala developer would choose this SDK over the Java SDK because:

* `Option`s, not nulls
* `Try`s, not exceptions
* `Future`s for all network operations
* No builder patterns, just case classes
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
import stellar.sdk._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
```

### Accounts

#### Creating and funding a test account

```
val kp = KeyPair.random
TestNetwork.fund(kp).foreach(println)
```

#### Checking the status of an account

```
val account: Future[AccountResp] = TestNetwork.account(kp)
account.foreach(println)
```

### Assets

#### Fetching a stream of all assets

```
import stellar.sdk.resp._
val assets: Future[Stream[AssetResp]] = TestNetwork.assets
assets.foreach(_.take(42).foreach(println))
```

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


## Aims

A Scala developer would choose to use this SDK because it aims to provide:

* Convenience
  * Asynchronous by default
  * Encapsulation of paged responses into `Stream`s
  * Case classes instead of builder patterns
  * Perform network operations on the terminal through the scala REPL

* Correctness
  * Generative testing using scalacheck with the goal of 100% test coverage
  * `Option`s, not nulls
  * `Try`s, not exceptions
  * Explicit type hierarchies instead of meaningful primitives


## Deprecation warning

At this stage, some classes and interfaces are likely to be refined. Minor releases may break backwards compatibility
with minimal notice until v1.0.0.


## Installation

In your `build.sbt`

```
resolvers += "scala-stellar-sdk-repo" at "https://dl.bintray.com/synesso/mvn"

libraryDependencies +=  "stellar.scala.sdk" %% "scala-stellar-sdk" % "0.1.2"
```

## Examples

The following examples use the [Ammonite REPL](http://ammonite.io/). After launching `amm`, fetch and import the
Stellar SDK for Scala.

```
interp.repositories() ++= Seq(coursier.MavenRepository("https://dl.bintray.com/synesso/mvn/"))

import $ivy.`stellar.scala.sdk::scala-stellar-sdk:0.1.2`
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
  println(s"""
    |Account ${resp.id}
    |  Last sequence was ${resp.lastSequence}.
    |  Balances: ${resp.balances.mkString(",")}
    """.stripMargin)
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

Project progress & roadmap can be viewed in the [projects tab](https://github.com/Synesso/scala-stellar-sdk/projects).

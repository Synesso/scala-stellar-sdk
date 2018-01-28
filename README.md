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

libraryDependencies +=  "stellar.scala.sdk" %% "scala-stellar-sdk" % "0.0.1.3"
```

## Examples

###

_todo_

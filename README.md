[![Travis](https://travis-ci.org/Synesso/scala-stellar-sdk.svg?branch=master)](https://travis-ci.org/Synesso/scala-stellar-sdk)
[![codecov](https://codecov.io/gh/Synesso/scala-stellar-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/Synesso/scala-stellar-sdk)

[![Download](https://api.bintray.com/packages/synesso/mvn/scala-stellar-sdk/images/download.svg)](https://bintray.com/synesso/mvn/scala-stellar-sdk/_latestVersion)

A Scala SDK for the Stellar network. It is a work in progress.

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
* Explicit type hierarchies instead of generic classes with meaningful primitives
* Perform network operations on the terminal through the scala console
* Test coverage: Generative test with scalacheck with a goal of 100% coverage

## Installation

In your `build.sbt`

```
resolvers += "synesso" at "https://dl.bintray.com/synesso/mvn"

libraryDependencies +=  "stellar.scala.sdk" %% "scala-stellar-sdk" % "0.0.1.3"
```

## Examples

###

_todo_

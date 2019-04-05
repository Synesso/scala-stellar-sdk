# [Stellar SDK for Scala](https://synesso.github.io/scala-stellar-sdk/)
by [Jem Mawson](https://keybase.io/jem)

[![Travis](https://img.shields.io/travis/Synesso/scala-stellar-sdk.svg)](https://travis-ci.org/Synesso/scala-stellar-sdk)
[![Coverage](https://img.shields.io/codecov/c/gh/Synesso/scala-stellar-sdk.svg)](https://codecov.io/gh/Synesso/scala-stellar-sdk)
[![Chat](https://img.shields.io/gitter/room/scala-stellar-sdk/community.svg)](https://gitter.im/0rora/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Issues](https://img.shields.io/github/issues/Synesso/scala-stellar-sdk.svg)](https://github.com/Synesso/scala-stellar-sdk/issues)


With the Stellar SDK for Scala, you can perform [Stellar](https://stellar.org/) operations from your Scala application. It provides the ability to access Stellar networks via any Horizon instance to build and submit transactions, query the state of the network and stream updates. You'll like this SDK, because it provides a more natural API for Scala developers than the official Java SDK.

You can do what you like with this software, as long as you include the required notices. See the [licence](https://github.com/Synesso/scala-stellar-sdk/blob/master/LICENSE) for more details.

## Getting Started

Add the [latest dependency](https://mvnrepository.com/artifact/io.github.synesso/scala-stellar-sdk) to your build tool. Here's how it might look in `build.sbt`

```scala
libraryDependencies += "io.github.synesso" %% "scala-stellar-sdk" % "0.6.2"
```

From there, it is a simple affair to create and fund a new account on the test network.

```scala
import stellar.sdk._
import scala.concurrent.ExecutionContext.Implicits.global
val kp = KeyPair.random
val response = TestNetwork.fund(kp)
```
You can try this right now [in your browser](https://scastie.scala-lang.org/ekyYbw9lS3GSnIkrbN2ozw).

Here's the code necessary to fetch an account's sequence number and submit a payment operation to the network.

```scala
implicit val network = TestNetwork
for {
  sourceAccount <- network.account(payerKeyPair)
  response <- model.Transaction(sourceAccount)
    .add(PaymentOperation(payeePublicKey, lumens(5000)))
    .sign(payerKeyPair)
    .submit()
} yield response
```

Please see the full [SDK documentation](https://synesso.github.io/scala-stellar-sdk) for further examples and full API details.


## Getting Help

There are a few ways to get help using this SDK.

1. Post your question to the [Stellar StackExchange](https://stellar.stackexchange.com/) and tag it with `scala-sdk`.
2. Ask in the [Scala-Stellar-SDK Gitter channel](https://gitter.im/scala-stellar-sdk/community).
3. Ask in the #dev_discussion channel in [Keybase](https://keybase.io/team/stellar.public).
4. Raise an issue on [this repository in GitHub](https://github.com/Synesso/scala-stellar-sdk/issues).


## Contributing

If you'd like to contribute new ideas, bug fixes or help to build out a planned feature, please take a look at the [current open issues](https://github.com/Synesso/scala-stellar-sdk/issues), or join the [gitter channel](https://gitter.im/scala-stellar-sdk/community) to discuss your thoughts.

If you've found this SDK helpful and you'd like to donate, the address is [![Donate](https://img.shields.io/keybase/xlm/jem.svg)](https://keybase.io/jem)

## Ack

_Thanks to the [Stellar Development Foundation](https://www.stellar.org/about/) for their support via their
[build challenge program](https://www.stellar.org/lumens/build)._

[![JetBrains](https://github.com/JetBrains/logos/blob/master/web/intellij-idea/intellij-idea.svg?sanitize=true)](https://www.jetbrains.com/?from=ScalaStellarSDK)


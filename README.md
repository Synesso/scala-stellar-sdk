# [Stellar SDK for Scala](https://synesso.github.io/scala-stellar-sdk/)

[![Travis](https://travis-ci.org/Synesso/scala-stellar-sdk.svg?branch=master)](https://travis-ci.org/Synesso/scala-stellar-sdk)
[![Coverage](https://img.shields.io/codecov/c/gh/Synesso/scala-stellar-sdk.svg)](https://codecov.io/gh/Synesso/scala-stellar-sdk)
[![Chat](https://img.shields.io/gitter/room/scala-stellar-sdk/community.svg)](https://gitter.im/scala-stellar-sdk/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Issues](https://img.shields.io/github/issues/Synesso/scala-stellar-sdk.svg)](https://github.com/Synesso/scala-stellar-sdk/issues)
![Supports Stellar Horizon v1.0.0-beta](https://img.shields.io/badge/Horizon-v1.0.0-beta-blue.svg)
![Supports Stellar Core v12](https://img.shields.io/badge/Core-v12-blue.svg)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

With the Stellar SDK for Scala, you can perform [Stellar](https://stellar.org/) operations from your Scala application. It provides the ability to access Stellar networks via any Horizon instance to build and submit transactions, query the state of the network and stream updates. You'll like this SDK, because it provides a more natural API for Scala developers than the official Java SDK.

You can do what you like with this software, as long as you include the required notices. See the [licence](https://github.com/Synesso/scala-stellar-sdk/blob/master/LICENSE) for more details.

## Getting Started

Add the JitPack resolver and the [latest dependency](https://jitpack.io/#Synesso/scala-stellar-sdk) to your build tool. Here's how it might look in `build.sbt`

```scala
resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.synesso" %% "scala-stellar-sdk" % "0.11.0"
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
  response <- model.Transaction(sourceAccount, timeBounds = TimeBounds.Unbounded, maxFee = lumens(100))
    .add(PaymentOperation(payeePublicKey, Amount.lumens(5000)))
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

To get started developing on the SDK itself, see the [DEV](https://github.com/Synesso/scala-stellar-sdk/blob/master/DEV.md) notes.

If you've found this SDK helpful and you'd like to donate, the address is [![Donate](https://img.shields.io/keybase/xlm/jem.svg)](https://keybase.io/jem)

## Ack

_Thanks to the [Stellar Development Foundation](https://www.stellar.org/about/) for their support via their
[build challenge program](https://www.stellar.org/lumens/build)._

[![JetBrains](https://github.com/JetBrains/logos/blob/master/web/intellij-idea/intellij-idea.svg?sanitize=true)](https://www.jetbrains.com/?from=ScalaStellarSDK)

by [Jem Mawson](https://keybase.io/jem)

# [Stellar SDK for Scala](https://github.com/synesso/scala-stellar-sdk/)
by [Jem Mawson](https://keybase.io/jem)

[![Travis](https://travis-ci.org/Synesso/scala-stellar-sdk.svg?branch=master)](https://travis-ci.org/Synesso/scala-stellar-sdk)
[![codecov](https://codecov.io/gh/Synesso/scala-stellar-sdk/branch/master/graph/badge.svg)](https://codecov.io/gh/Synesso/scala-stellar-sdk)
[![Join chat at https://gitter.im/scala-stellar-sdk/community](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scala-stellar-sdk/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


With the Stellar SDK for Scala, you can perform [Stellar](https://stellar.org/) operations from your Scala application. It provides the ability to access Stellar networks via any Horizon instance to build and submit transactions, query the state of the network and stream updates. You'll like this SDK, because it provides a more natural API for Scala developers than the official Java SDK.

You can do what you like with this software, as long as you include the required notices. See the [licence](https://github.com/Synesso/scala-stellar-sdk/blob/master/LICENSE) for more details.

## Getting Started

Add the [latest dependency](https://mvnrepository.com/artifact/io.github.synesso/scala-stellar-sdk) to your build tool. Here's how it might look in `build.sbt`

```
libraryDependencies += "io.github.synesso" %% "scala-stellar-sdk" % "0.5.1"
```

From there, it is a simple affair to create and fund a new account on the test network.

```
import stellar.sdk._
import scala.concurrent.ExecutionContext.Implicits.global
val kp = KeyPair.random
val response = TestNetwork.fund(kp)
```

Here's the code necessary to fetch an account's sequence number and submit a payment operation to the network.

```
implicit val network = TestNetwork
for {
  sourceAccount <- network.account(payerKeyPair)
  response <- model.Transaction(sourceAccount)
    .add(PaymentOperation(payeePublicKey, lumens(5000)))
    .sign(payerKeyPair)
    .submit()
} yield response
```

Please see the full [SDK documentation](https://synesso.github.io/scala-stellar-sdk) for complete details.


## Getting Help

There are a few ways to get help using this SDK.

1. Post your question to the [Stellar StackExchange](https://stellar.stackexchange.com/) and tag it with `scala-sdk`.
2. Ask in the [Scala-Stellar-SDK Gitter channel](https://gitter.im/scala-stellar-sdk/community).
3. Ask in the #dev_discussion channel in [Keybase](https://keybase.io/team/stellar.public).
4. Raise an issue on [this repository in GitHub](https://github.com/Synesso/scala-stellar-sdk/issues).


## Contributing

If you'd like to contribute new ideas, bug fixes or help to build out a planned feature, please take a look at the [current open issues](https://github.com/Synesso/scala-stellar-sdk/issues), or join the [gitter channel](https://gitter.im/scala-stellar-sdk/community) to discuss your thoughts.

## Ack

_Thanks to the [Stellar Development Foundation](https://www.stellar.org/about/) for their support via their
[build challenge program](https://www.stellar.org/lumens/build)._

[![JetBrains](https://github.com/JetBrains/logos/blob/master/web/intellij-idea/intellij-idea.svg?sanitize=true)](https://www.jetbrains.com/?from=ScalaStellarSDK)


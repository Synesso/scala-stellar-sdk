---
material.color.primary: indigo
material.color.accent: blue
material.logo.icon: near_me
material.copyright: © Jem Mawson. Licensed under Apache 2.0
---

# Stellar SDK for Scala

This is the SDK for performing [Stellar](https://www.stellar.org/) operations via Scala. It provides the ability to
access Stellar networks via Horizon instances to build and submit transactions, query the state of the network and
stream updates.

Scala developers may prefer to use this SDK because:

* it has a simple, Scala-centric API
* its operations are asynchronous by default
* it uses explicit type hierarchies instead of primitives
* it is tested to near 100% coverage with generative testing

The code throughout this documentation is compiled against Scala $scalaBinaryVersion$. (The library also supports
Scala 2.11).


## Quick-start

Add the SDK via your dependency management tool.

@@dependency[sbt,Maven,Gradle] {
  group="$organization$"
  artifact="$name$"
  version="$version$"
}

Creating an account on the public test network.

@@snip [SessionTestAccount.scala](../../it/scala/stellar/sdk/SessionTestAccount.scala) { #friendbot_example }

Fetching the details of an account.

@@snip [SequentialIntegrationSpec.scala](../../it/scala/stellar/sdk/SequentialIntegrationSpec.scala) { #account_details_example }

Submitting a payment.





## Deprecation warning

At this stage, some classes and interfaces are likely to be refined. Minor releases may break backwards compatibility
with minimal notice until v1.0.0.


-- todo --

Release notes are found at [Github releases](https://github.com/akka/alpakka/releases).


## Contributing

Please feel free to contribute to Alpakka by reporting issues you identify, or by suggesting changes to the code. Please refer to our [contributing instructions](https://github.com/akka/alpakka/blob/master/CONTRIBUTING.md) and our [contributor advice](https://github.com/akka/alpakka/blob/master/contributor-advice.md) to learn how it can be done.

We want Akka and Alpakka to strive in a welcoming and open atmosphere and expect all contributors to respect our [code of conduct](https://github.com/akka/alpakka/blob/master/CODE_OF_CONDUCT.md).



@@ toc { .main depth=2 }

@@@ index


@@@

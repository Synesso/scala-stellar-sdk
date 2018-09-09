---
material.color.primary: indigo
material.color.accent: blue
material.logo.icon: near_me
material.copyright: © Jem Mawson. Licensed under Apache 2.0
---

# Stellar SDK for Scala

This is the SDK for performing [Stellar](https://www.stellar.org/) operations via Scala. It provides the ability to
access the Stellar network via Horizon instances to build and submit transactions, query the state of the network and
stream updates.

Scala developers may prefer to use this SDK because:

* it has a simple, Scala-centric API
* its operations are asynchronous by default
* it uses explicit type hierarchies instead of primitives
* it is tested to near 100% coverage with generative testing

The code throughout this documentation is compiled against Scala $scalaBinaryVersion$.


## Quick-start

Add the SDK via your dependency management tool.

@@dependency[sbt,Maven,Gradle] {
  group="$organization$"
  artifact="$name$_2.12"
  version="$version$"
}

Creating an account on the public test network.

@@snip [FriendbotSpec.scala](../../it/scala/stellar/sdk/FriendbotSpec.scala) { #friendbot_example }

Fetching the details of an account.

@@snip [LocalNetworkIntegrationSpec.scala](../../it/scala/stellar/sdk/LocalNetworkIntegrationSpec.scala) { #account_details_example }

Submitting a payment.

@@snip [NetworkSpec.scala](../../it/scala/stellar/sdk/LocalNetworkIntegrationSpec.scala) { #payment_example }

For more detailed coverage, continue by reading about @ref:[Transacting](transacting.md).

## API

Please enjoy the [scaladoc](api/stellar/sdk) for this release.


> ### Deprecation warning

> At this stage, classes and interfaces are likely to be refined. Minor releases may break backwards compatibility
with minimal notice until v1.0.0.


## Contributing

Contributions are warmly welcomed. Please feel free to contribute by reporting [issues](https://github.com/Synesso/scala-stellar-sdk/issues)
you find, or by suggesting changes to the code. Or feel free to add your own features/issues to that list.

Let's chat about any of this on the [Stellar-public Slack](https://stellar-public.slack.com/) #dev channel. My username
is [@jem](https://keybase.io/jem).


@@@ index

* [Transacting](transacting.md)
* [Queries](queries.md)
* [Sources](sources.md)

@@@

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

Scala developers may prefer to use this SDK because it provides a more natural API for Scala developers than the
official Java SDK

The code throughout this documentation is compiled against Scala $scalaBinaryVersion$.


## Quick-start

Add the jitpack resolver.

`resolvers += "jitpack" at "https://jitpack.io"`

Then, add the SDK via your dependency management tool.

@@dependency[sbt,Maven,Gradle] {
  group="$organization$"
  artifact="$name$_2.13"
  version="$version$"
}

Creating an account on the public test network.

@@snip [FriendBotSpec.scala](../../it/scala/stellar/sdk/FriendBotSpec.scala) { #friendbot_example }

Fetching the details of an account.

@@snip [LocalNetworkIntegrationSpec.scala](../../it/scala/stellar/sdk/LocalNetworkIntegrationSpec.scala) { #account_details_example }

Submitting a payment.

@@snip [NetworkSpec.scala](../../it/scala/stellar/sdk/LocalNetworkIntegrationSpec.scala) { #payment_example }

For more detailed coverage, continue by reading about @ref:[KeyPairs](key_pairs.md).

## API

Please enjoy the [scaladoc](latest/api/stellar/sdk) for this release.


> ### Deprecation warning

> At this stage, classes and interfaces are likely to be refined. Minor releases may break backwards compatibility
with minimal notice until v1.0.0.

Check the [CHANGELOG](https://github.com/Synesso/scala-stellar-sdk/blob/master/CHANGELOG.md#changelog) for details of
breaking changes.


## Contributing

Contributions are warmly welcomed. Please feel free to contribute by reporting [issues](https://github.com/Synesso/scala-stellar-sdk/issues)
you find, or by suggesting changes to the code. Or feel free to add your own features/issues to that list.

You can [contact me on KeyBase](https://keybase.io/jem/chat).


@@@ index

* [Transacting](transacting.md)
* [Key Pairs](key_pairs.md)
* [Queries](queries.md)
* [Sources](sources.md)
* [Domains](domains.md)
* [Authentication](authentication.md)

@@@

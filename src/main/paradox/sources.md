# Sources

Many of the of the APIs made available via @ref:[Queries](queries.md) are also available in a streaming mode. Access
to the streaming endpoints is made available on the @scaladoc[Network](stellar.sdk.Network) interface by way of
`*Source` methods. Whereas queries provide access to historical data, sources can be used to subscribe to updates from
the network in near real-time.

`Sources` are a concept native to [Akka Streams](https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html#introduction).
They provide resilient access to upstream data and support backpressure should the data be arriving faster than the
application is capable of processing.

As a primer, sources may be used as a method for subscribing to updates. For example:

@@snip [NetworkSpec.scala](../../test/scala/stellar/sdk/NetworkSpec.scala) { #transaction_source_examples }


Like queries, sources fall into several categories.

### Transactions

Transactions are the fundamental unit of change in the network and are composed of at least one and at most 100 operations.
These sources stream validated transactions, in the form of @scaladoc[TransactionHistoryResp](stellar.sdk.resp.TransactionHistoryResp)onses
(as opposed to transactions that are composed and submitted to the network).


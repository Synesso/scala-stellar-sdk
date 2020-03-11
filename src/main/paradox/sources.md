# Sources

Many of the of the APIs made available via @ref:[Queries](queries.md) are also available in a streaming mode. Access
to the streaming endpoints is made available on the @apidoc[Network] interface by way of
`*Source` methods. Whereas queries provide access to historical data, sources can be used to subscribe to updates from
the network in near real-time.

`Sources` are a concept native to [Akka Streams](https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html#introduction).
They provide resilient access to upstream data and support backpressure should the data be arriving faster than the
application is capable of processing.

Before sources can be used, an actor system and materializer need to be brought into implicit scope.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #sources_implicit_setup }

Once done, sources may be used as a method for subscribing to updates. For example:

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #transaction_source_examples }


Like queries, sources fall into several categories.

### Effects

@apidoc[EffectResponse]s are the changes that have been effected on the network as a result of
operations successfully processed.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #effect_source_examples }


### Ledgers

@apidoc[LedgerResponse]s represent the state of the network at any time. They are created
sequentially as the state of the network changes.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #ledger_source_examples }


### Offers

@apidoc[OfferResponse]s can be issued by accounts to buy or sell assets. Sources for offers
is available only by account.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #offer_source_examples }


### Operations

@apidoc[Operation]s are changes to the ledger. They represent the action, as opposed to
the effects resulting from the action.

Operations returned by these queries are wrapped in the @apidoc[Transacted] type. This indicates
that the operation has been part of a successful transaction, and provides details about that transaction.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #operation_source_examples }


### OrderBooks

@apidoc[OrderBook]s include all the offers to buy or sell a specific asset. The source for an
orderbook will present offers for that pair.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #orderbook_source_examples }


### Payments

@apidoc[PayOperation]s are the subset of Operations that cause payments to be made to an
account. This is similar to the [Operations](#operations) query methods, but will only return `CreateAccount` and
`Payment` operations.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #payment_source_examples }


### Transactions

Transactions are the fundamental unit of change in the network and are composed of at least one and at most 100 operations.
These sources stream validated transactions, in the form of @apidoc[TransactionHistory]
responses (as opposed to transactions that are composed and submitted to the network).

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #transaction_source_examples }

Continue reading to learn how to query for information on organizations that use the Stellar network via @ref:[Domains](domains.md).

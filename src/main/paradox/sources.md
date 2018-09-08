# Sources

Many of the of the APIs made available via @ref:[Queries](queries.md) are also available in a streaming mode. Access
to the streaming endpoints is made available on the @scaladoc[Network](stellar.sdk.Network) interface by way of
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

### Offers

@scaladoc[Offers](stellar.sdk.resp.OfferResp) can be issued by accounts to buy or sell assets. Sources for offers
is available only by account.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #offer_source_examples }


### Operations

@scaladoc[Operations](stellar.sdk.resp.Operation) are changes to the ledger. They represent the action, as opposed to
the effects resulting from the action.

Operations returned by these queries are wrapped in the @scaladoc[Transacted](stellar.sdk.op.Transacted) type. This indicates
that the operation has been part of a successful transaction, and provides details about that transaction.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #operation_source_examples }


### OrderBooks

@scaladoc[OrderBooks](stellar.sdk.OrderBook) include all the offers to buy or sell a specific asset. The source for an
orderbook will present offers for that pair.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #orderbook_source_examples }


### Payments

@scaladoc[Payments](stellar.sdk.op.PayOperation) are the subset of Operations that cause payments to be made to an
account. This is similar to the [Operations](#operations) query methods, but will only return `CreateAccount` and
`Payment` operations.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #payment_source_examples }


### Transactions

Transactions are the fundamental unit of change in the network and are composed of at least one and at most 100 operations.
These sources stream validated transactions, in the form of @scaladoc[TransactionHistoryResp](stellar.sdk.resp.TransactionHistoryResp)onses
(as opposed to transactions that are composed and submitted to the network).

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #transaction_source_examples }

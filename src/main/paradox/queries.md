# Queries

A convenient HTTP API for accessing Stellar is provided by the Stellar Development Foundation (SDF) through a component called
[Horizon](https://www.stellar.org/developers/reference/). The Scala Stellar SDK provides access to these endpoints via
methods on the @scaladoc[Network](stellar.sdk.Network) interface. These methods mirror the REST API provided by Horizon.

The SDF supports two main networks, the primary public network and another for testing. These are represented separately by the objects
@scaladoc[PublicNetwork](stellar.sdk.PublicNetwork$) and @scaladoc[TestNetwork](stellar.sdk.TestNetwork$).

> If it is your use-case that you are deploying a separate, private Stellar network, then you can implement
the `Network` trait to provide access to the Horizon endpoints on your network.

Queries fall into several categories.

### Accounts

@scaladoc[Account](stellar.sdk.resp.AccountResp)s are the entities through which users can interact with the network.
They are represented by a key pair. Account details can be found given an account's @scaladoc[KeyPair](stellar.sdk.KeyPair)
or @scaladoc[PublicKey](stellar.sdk.PublicKey).

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #account_query_examples }


### Assets

@scaladoc[Assets](stellar.sdk.resp.AssetResp) are the items that are traded on the network. They can be searched for by
their `code`, `issuer`, neither or both.

By default the results will be in ascending order from the earliest record. This behaviour can be modified with the
`cursor` and `order` parameters.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #asset_query_examples }


### Effects

@scaladoc[Effects](stellar.sdk.resp.EffectResp) are the changes that have been effected on the network as a result of
operations successfully processed.

By default the results will be in ascending order from the earliest record. This behaviour can be modified with the
`cursor` and `order` parameters.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #effect_query_examples }


### Fee Statistics

When transacting, it is possible to nominate a fee that is greater than the base fee. Transactions paying higher fees
are prioritised at times of high load.

This parameterless endpoint provides statistics about the fees paid during the last known ledger.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #fee_stats_query_example }

### Ledgers

@scaladoc[Ledgers](stellar.sdk.resp.LedgerResp) represent the state of the network at any time. They are created
sequentially as the state of the network changes.

It is possible to stream all ledgers or query for a specific ledger by its sequential id. Each returned value provides
meta-data about the changes in that ledger, as well as a summary of the network at that point of time.

By default the results will be in ascending order from the earliest record. This behaviour can be modified with the
`cursor` and `order` parameters.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #ledger_query_examples }


### Network Info

Meta information about the network itself is available via the `info()` endpoint. Data provided in the response includes
the network passphrase, software versions for Horizon and Core, the current and max supported core protocol versions,
and the earliest and latest ledger ids.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #network_info_example }


### Offers

@scaladoc[Offers](stellar.sdk.resp.OfferResp) can be issued by accounts to buy or sell assets. Querying for offers
is available only by account. Additional offers are found by searching the `OrderBook`.

By default the results will be in ascending order from the earliest record. This behaviour can be modified with the
`cursor` and `order` parameters.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #offer_query_examples }


### Operations

@scaladoc[Operations](stellar.sdk.resp.Operation) are changes to the ledger. They represent the action, as opposed to
the effects resulting from the action.

Operations returned by these queries are wrapped in the @scaladoc[Transacted](stellar.sdk.model.op.Transacted) type. This indicates
that the operation has been part of a successful transaction, and provides details about that transaction.

By default the results will be in ascending order from the earliest record. This behaviour can be modified with the
`cursor` and `order` parameters.

There are several ways to search for and filter operations.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #operation_query_examples }


### OrderBook

@scaladoc[OrderBooks](stellar.sdk.OrderBook) include all the offers to buy or sell a specific asset. They show the
depth limited to the value of the `limit` param, which defaults to `20`.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #orderbook_query_examples }


### Payments

@scaladoc[Payments](stellar.sdk.model.op.PayOperation) are the subset of Operations that cause payments to be made to an
account. This is similar to the [Operations](#operations) query methods, but will only return `CreateAccount` and
`Payment` operations.

By default the results will be in ascending order from the earliest record. This behaviour can be modified with the
`cursor` and `order` parameters.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #payment_query_examples }


### Payment Paths

In order for a cross-token payment to find the most cost-efficient path through the offers in the distributed ledger, it
is necessary to query the network for the available pathways, given the current orderbooks.

@scaladoc[Payment Paths](stellar.sdk.model.PaymentPath) document the source & destination amounts - along with any
intermediate assets that would fulfil a path payment.


### Trades

@scaladoc[Trades](stellar.sdk.Trade) are created when offers in an orderbook are partially or fully matched.

By default the results will be in ascending order from the earliest record. This behaviour can be modified with the
`cursor` and `order` parameters.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #trade_query_examples }


### Trade Aggregations

@scaladoc[TradeAggregations](stellar.sdk.TradeAggregation) are summaries of historical trade data for a given asset pair,
time period and time resolution.

By default the results will be in ascending order from the earliest record. This behaviour can be modified with the
`cursor` and `order` parameters.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #trade_query_examples }


### Transactions

By default the results will be in ascending order from the earliest record. This behaviour can be modified with the
`cursor` and `order` parameters.

Transactions are the fundamental unit of change in the network and are composed of at least one and at most 100 operations.
These queries return validated transactions, in the form of @scaladoc[TransactionHistory](stellar.sdk.model.result.TransactionHistory)
responses (as opposed to transactions that are composed and submitted to the network).

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #transaction_query_examples }

Continue reading to learn how to subscribe and respond to future events via @ref:[Sources](sources.md).

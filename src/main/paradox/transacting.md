# Transacting

[Transactions](https://www.stellar.org/developers/guides/concepts/transactions.html) are how changes such as payments,
offers to trade or account creation are made to the network's ledger.

## Creating

Every transaction must originate from an existing account on the network and correctly specify the next
[sequence number](https://www.stellar.org/developers/guides/concepts/accounts.html#sequence-number).
Creating a new `Transaction` instance requires these two values, wrapped in an @scaladoc[Account](stellar.sdk.Account).

Additionally, a `Network` must be implicit in scope. The choice of network will affect how the transaction is serialised.

@@snip [FriendbotSpec.scala](../../it/scala/stellar/sdk/FriendbotSpec.scala) { #new_transaction_example }

### Sequence Number

The sequence number for a new account is the id of the ledger in which is was created. The id increments by one with every
submitted transaction. In simple architectures, it is possible to keep track of the next sequence number without querying the network.
However, if this is not possible, or the number is unknown, you can directly substitute the response from the
@ref:[account query](queries.md#accounts) into the `Transaction` constructor.

@@snip [LocalNetworkIntegrationSpec.scala](../../it/scala/stellar/sdk/LocalNetworkIntegrationSpec.scala) { #payment_example }

As this example shows, transactions require additional data before they can be successfully processed.

### Operations

Without any @scaladoc[Operation](stellar.sdk.op.Operation)s, a transaction is not very useful. There can be as few as
one and as many and one hundred operations added to each Transaction. These can be provided when constructing the
Transaction.

@@snip [NetworkSpec.scala](../../test/scala/stellar/sdk/NetworkSpec.scala) { #transaction_createwithops_example }

Or they can be added afterwards.

@@snip [NetworkSpec.scala](../../test/scala/stellar/sdk/NetworkSpec.scala) { #transaction_addops_example }

The available operations are:

* @scaladoc[AccountMerge](stellar.sdk.op.AccountMergeOperation)
* @scaladoc[AllowTrust](stellar.sdk.op.AllowTrustOperation)
* @scaladoc[BumpSequence](stellar.sdk.op.BumpSequenceOperation)
* @scaladoc[ChangeTrust](stellar.sdk.op.ChangeTrustOperation)
* @scaladoc[CreateAccount](stellar.sdk.op.CreateAccountOperation)
* @scaladoc[CreateOffer](stellar.sdk.op.CreateOfferOperation)
* @scaladoc[CreatePassiveOffer](stellar.sdk.op.CreatePassiveOfferOperation)
* @scaladoc[DeleteData](stellar.sdk.op.DeleteDataOperation)
* @scaladoc[DeleteOffer](stellar.sdk.op.DeleteOfferOperation)
* @scaladoc[Inflation](stellar.sdk.op.InflationOperation)
* @scaladoc[PathPayment](stellar.sdk.op.PathPaymentOperation)
* @scaladoc[Payment](stellar.sdk.op.PaymentOperation)
* @scaladoc[SetOptions](stellar.sdk.op.SetOptionsOperation)
* @scaladoc[UpdateOffer](stellar.sdk.op.UpdateOfferOperation)
* @scaladoc[WriteData](stellar.sdk.op.WriteDataOperation)

Operations need not originate from the same account as the transaction. In this way a single transaction can be issued that
affects multiple accounts. This enables techniques such as the
[channel pattern](https://www.lumenauts.com/blog/boosting-tps-with-stellar-channels). Each operation has an optional
constructor parameter `sourceAccount: Option[PublicKey]` where the source account can be specified.

### Signatures

Before a transaction will be accepted by the network, it must be signed with at least one key. In the most basic case,
the transaction only needs to be signed by the source account. This is done by calling `.sign(KeyPair)`.

@@snip [NetworkSpec.scala](../../test/scala/stellar/sdk/NetworkSpec.scala) { #transaction_signing_example }

It may be that the source account has been modified to require more than one signature. Or, as mentioned earlier, one or
more of the operations may affect other accounts. In either of these cases, the transaction will not be valid until it
has received all necessary signatures.

@@snip [NetworkSpec.scala](../../test/scala/stellar/sdk/NetworkSpec.scala) { #joint_transaction_signing_example }

Additionally, a transaction will fail if it has too many signatures.

## Submitting

Once a transaction is signed (and therefore is of type `SignedTransaction`) it can be submitted to the network.

@@snip [NetworkSpec.scala](../../test/scala/stellar/sdk/NetworkSpec.scala) { #transaction_submit_example }

The eventual resulting @scaladoc[TransactionPostResp](stellar.sdk.resp.TransactionPostResp) contains metadata about the
processed transaction, including the full results encoded as [XDR](https://www.stellar.org/developers/guides/concepts/xdr.html).
Additionally, the XDR can be decoded on the fly by calling the relevant convenience methods.

@@snip [NetworkSpec.scala](../../test/scala/stellar/sdk/NetworkSpec.scala) { #transaction_response_example }

## XDR

Transactions can be serialized to a base64-encoding of their XDR form. This is a strictly-defined format for transactions
that is compatible across all supporting Stellar libraries and tooling. Given this, it is possible to save and load
transaction state via XDR strings.

@@snip [NetworkSpec.scala](../../test/scala/stellar/sdk/TransactionSpec.scala) { #xdr_serde_example }

Transactions with signatures are a different data structure (signatures are included in an envelope along with the transaction)
and need to be decoded via a similar method on `SignedTransaction`.

@@snip [NetworkSpec.scala](../../test/scala/stellar/sdk/TransactionSpec.scala) { #xdr_signed_serde_example }

Continue reading to learn how to obtain historical data from network via @ref:[Queries](queries.md).

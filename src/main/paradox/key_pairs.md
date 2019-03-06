# Key Pairs

Accounts in Stellar are identified by a cryptographic key pair. 

The public key, also known as the __account id__, can safely be shared with anybody. It is 52 characters long and begins 
with a `G`.

The private key, referred to as the __secret seed__, should be kept private & secure. It is also 52 characters long, but 
begins with an `S`.

For example, the following is a randomly generated account key pair. (This is an example only, do not use this.) 

```
Account id:  GD2HMF3BKITMXISCPTU7VVTFXDY5WSQK4QNIUATNCXVKBNWZP7FWZOXG
Secret Seed: SDHXK2UNHTXVW2MZSOVOPYUKVXD3PEVKMNQZZGPODQMR67YTKWMOC732
```

The knowledge of a key pair does not imply that the account exists on any Stellar network. In other words, when you call
`KeyPair.random`, this does not create an account. To do that, you need to issue a transaction that contains a 
`CreateAccountOperation`, as explained in @ref:[Transacting](transacting.md).

There are actually two kinds of `KeyPair` objects in the SDK.

* @scaladoc[KeyPair](stellar.sdk.KeyPair) represents the public and secret components, and can be used for operations 
    that require proof of the secret portion, such as transaction signing.
* @scaladoc[PublicKey](stellar.sdk.PublicKey) represents only the public component. This is used to refer to accounts 
    other than your own, such as the counterparty of a payment.
    
As the account id can be derived from the secret seed, there is no object which models the secret seed without the 
account id. 
      
The SDK provides several ways to create or resolve `KeyPair`s and `PublicKey`s.

### Randomly

A random `KeyPair` is generated when you call `KeyPair.random`.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #keypair_randomly }

The chance of receiving a key pair that has been seen before is [so miniscule](https://stellar.stackexchange.com/a/772/111)
that you might reasonably assume that any randomly generated pair is effectively unique.

### By Key

If you know the secret seed (private key), then you can reconstitute the `KeyPair`.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #keypair_from_secret_seed }


If you only know the account id (public key), you can create the `PublicKey`.

@@snip [DocExamples.scala](../../test/scala/stellar/sdk/DocExamples.scala) { #keypair_from_accountid } 


### By Passphrase

As a substitute for a valid secret seed, any passphrase can be used to deterministically derive a `KeyPair`. 

@@snip [KeyPairSpec.scala](../../test/scala/stellar/sdk/KeyPairSpec.scala) { #keypair_from_passphrase }

### By Federated Address

As a convenience, accounts may also be identified by a human-readable address known as a 
[Federated Address](https://www.lumenauts.com/guides/what-are-federated-stellar-addresses). These are in the format
`NAME*DOMAIN`.

Federated addresses can be resolved using the `fromAddress` method. Because the actual public key is retrieved from the 
network, this method returns a `Future[PublicKey]` and may fail.

@@snip [KeyPairSpec.scala](../../test/scala/stellar/sdk/KeyPairSpec.scala) { #keypair_from_federated_address }
  

That's all you need to know about `KeyPair`s. Continue reading to learn about how to inspect Stellar networks via 
@ref:[Queries](queries.md).

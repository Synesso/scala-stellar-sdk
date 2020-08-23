# Authentication

In [SEP-0010](https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0010.md#preamble) Stellar defines
a mechanism for using the cryptographic properties of transactions in order to present and fulfil an authentication
challenge.

The server constructs a challenge with an @apidoc[AuthChallenger]. The challenge can be serialised to and deserialised
from JSON.

@@snip [ChallengeSpec.scala](../../test/scala/stellar/sdk/auth/ChallengeSpec.scala) { #challenge_to_from_json_example } 

The client can meet the challenge by signing the presented transaction. The server can then  

@@snip [ChallengeSpec.scala](../../test/scala/stellar/sdk/auth/ChallengeSpec.scala) { #auth_challenge_success_example }

It is important that the client validate the properties of the challenge transaction before signing and returning.
For example, the transaction should have a sequence number of zero to prevent it from being submittable to the network.
See the [SEP-0010 specification](https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0010.md#abstract)
for up-to-date requirements.
  


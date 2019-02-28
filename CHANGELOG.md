# Changelog

As this project is pre 1.0, breaking changes may happen for minor version bumps. A breaking change will get clearly notified in this log.

## Next version

### Added

- Added `Network.feeStats()` method to return the fee statistics from the most recent ledger, as per the [`fee_stats`
    endpoint](https://www.stellar.org/developers/horizon/reference/endpoints/fee-stats.html).
    [#58](https://github.com/Synesso/scala-stellar-sdk/issues/58)

- Included client request headers `X-Client-Name` and `X-Client-Version` so that Horizon instances can tell when a 
    request comes from the Scala SDK. [#59](https://github.com/Synesso/scala-stellar-sdk/issues/59)
    

## 0.5.2

### Changed

- Change to `LedgerResponse` fields to match updates in Horizon v0.17.0. The fields `successTransactionCount` and
    `failureTransactionCount` were added. The field `transactionCount` remains as a method defined in terms
    of the new fields. [#57](https://github.com/Synesso/scala-stellar-sdk/issues/57)

### Fixed

- `AccountResponse` parsing. The JSON field `public_key` was replaced with `key`. [#57](https://github.com/Synesso/scala-stellar-sdk/issues/57)


## 0.5.1

* Introduction of new value on `TransactionResponses`  - `def sequenceUpdated: Boolean`. This indicates whether the client
    should consider the sequence number to have incremented as a result of the transaction posting. It's always
    true when the transaction was successful. But it's only sometimes true if the transaction failed as it depends upon
    whether the transaction passed pre-consensus validation.

* Attempts to construct a KeyPair with a bad account id will now throw `InvalidAccountId`.

* Attempts to construct a KeyPair with a bad secret seed will now throw `InvalidSecretSeed`.


## 0.5.0

### Breaking Changes
* Restructuring of transaction submission response types.
    * `TransactionPostResponse` (abstract)
        * `TransactionApproved`
        * `TransactionRejected`

* Complete removal of generated XDR classes in favour of domain objects handling their own XDR encoding.
    This means operations such as `TransactionResult.decodeXDR` will now return instances of domain objects
    newly created in this release which can be used in pattern matches. These classes incorporate all of the 
    XDR encoded information in a user-friendly form and have the following hierarchy:
    * `TransactionResult` (trait)
        * `TransactionSuccess`
        * `TransactionNotSuccessful` (trait)
            * `TransactionFailure`
            * `TransactionNotAttempted`

* Most classes have changed packages to better expose the common user-facing classes.
    * `stellar.sdk` contains commonly used classes `KeyPair`, `Network` and
      the concrete Network instances `PublicNetwork` and `TestNetwork`.
    * `stellar.sdk.model` - classes used to model the request and response domain objects.
    * `stellar.sdk.model.op` - domain objects specific to operations.
    * `stellar.sdk.model.response` - objects representing Horizon responses.
    * `stellar.sdk.model.result` - domain objects specific to transaction submission results.
    * `stellar.sdk.model.xdr` - helper classes used by domain objects for XDR serialisation.

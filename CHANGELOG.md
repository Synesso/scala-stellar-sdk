# Changelog

As this project is pre 1.0, breaking changes may happen for minor version bumps. A breaking change will get clearly notified in this log.

## 0.10.0

- [#156](https://github.com/Synesso/scala-stellar-sdk/issues/156) Adds support for Core v12 and Horizon v0.22.1

### Breaking changes

- In line with the changes to the core protocol:
  - `PathPaymentOperation` has been renamed to `PathPaymentStrictReceiveOperation`
  - `PathPaymentStrictSendOperation` has been added.

  These operations are differentiated by which party (sender or receiver) will have their funds
  explicitly stated. The other party will obtain the best rate available via the path supplied.

## 0.9.0

### Breaking changes

- [#121](https://github.com/Synesso/scala-stellar-sdk/issues/121) Transaction fees are no longer implicit. The initiator
        of a transaction needs to explicitly specify the maximum fee that they are prepared to pay.
- [#129](https://github.com/Synesso/scala-stellar-sdk/issues/129) Timebounds are no longer implicit either. The initiator
        of a transaction needs to explicitly specify the timebounds of the transaction. The constant
        `TimeBounds.Unbounded` is introduced to provide a short-cut for unbounded behaviour.

### Added

- [#96](https://github.com/Synesso/scala-stellar-sdk/issues/96) Full support for `well-known.toml` fields in the 
        `DomainInfo` class. Previously only `FEDERATION_SERVER` was supported. Now all fields are available.
- `TimeBounds` can be defined in terms of a timeout from 'now', with `TimeBounds.timeout(Duration)`.
- [#66](https://github.com/Synesso/scala-stellar-sdk/issues/66) Transactions can be signed with any arbitrary byte array.
        This provides the ability to match a hash signer with shared data. (See 
        [Hash(x) signing](https://www.stellar.org/developers/guides/concepts/multi-sig.html#hashx) for more details).

## 0.8.0

### Breaking changes

- `AccountResponse` now models account data values as `Array[Byte]`, not `String` (See below).
- Due to [this bug in Horizon](https://github.com/stellar/go/issues/1381), the `validBefore` and `validAfter` fields of 
    `TransactionHistory` may appear as `None` when they were in fact present. This document will note when these fields
    become reliable again.

### Added

- [#33](https://github.com/Synesso/scala-stellar-sdk/issues/33) Support for pathfinding endpoint `/paths`.
- [#35](https://github.com/Synesso/scala-stellar-sdk/issues/35) Support for trade aggregations endpoint `/trade_aggregations`.
- [#53](https://github.com/Synesso/scala-stellar-sdk/issues/53) On-the-fly decoding of transaction meta-info about the
    entries affecting the ledger.
- [#120](https://github.com/Synesso/scala-stellar-sdk/issues/120) On-the-fly decoding of transaction fee meta-info.

### Changed
- [#92](https://github.com/Synesso/scala-stellar-sdk/issues/92) Horizon Release v0.18.0 compatibility. Added fields
       `maxFee` and `feeCharged` to transaction responses. The field `feePaid` is now deprecated and will be removed
       in a future release.
- [#51](https://github.com/Synesso/scala-stellar-sdk/issues/51) Data associated with an account is modelled as a byte
        array and parsed from Horizon responses as Base64-encoded Strings.

## 0.7.1

### Added

- [#74](https://github.com/Synesso/scala-stellar-sdk/issues/74) Failed network calls to Horizon will now automatically
        retry several times.

### Fixed

- [#86](https://github.com/Synesso/scala-stellar-sdk/issues/86) Horizon response of `TooManyRequests` will result in
        a `HorizonRateLimitExceeded` response. That exception type includes the duration until the next rate limit
        window opens.
- [#70](https://github.com/Synesso/scala-stellar-sdk/issues/70) Account responses now include account data.
- [#76](https://github.com/Synesso/scala-stellar-sdk/issues/76) MemoIds are parsed as Longs and accept zero as a value.

## 0.7.0

### Breaking changes

- Renamed types `{Create|Delete|Update}OfferOperation` to `{Create|Delete|Update}SellOfferOperation`.
- Renamed type `CreatePassiveOfferOperation` to `CreatePassiveSellOfferOperation`.

### Added

- [#77](https://github.com/Synesso/scala-stellar-sdk/issues/77) Supports protocol v11.
    - Renames `{Create|Delete|Update}OfferOperation` to `{Create|Delete|Update}SellOfferOperation`.
    - Renames `CreatePassiveOfferOperation` to `CreatePassiveSellOfferOperation`.
    - Introduces `{Create|Delete|Update}BuyOfferOperation`.
- [#78](https://github.com/Synesso/scala-stellar-sdk/issues/78) Parse `manage_sell_offer` JSON responses to
    provide compatibility with Horizon v0.18.0.

### Fixed

- [#76](https://github.com/Synesso/scala-stellar-sdk/issues/76) `MemoId` is parsed as unsigned Long.
- [#84](https://github.com/Synesso/scala-stellar-sdk/issues/84) Return hash memos are handled explicitly.

## 0.6.4

### Fixed

 - Avoid rounding errors in `EffectTrustLine{Created|Updated}`. Separate `limit` and `asset` fields where merged into a single `IssuedAmount`.

## 0.6.3

### Added

- [#68](https://github.com/Synesso/scala-stellar-sdk/issues/68) Added `base_offer_id` and `counter_offer_id` fields to the 
    `Trade` object. These fields were introduced with Horizon v0.15.0
- [#69](https://github.com/Synesso/scala-stellar-sdk/issues/69) Added support for optional `valid_before` and 
    `valid_after` fields on `TransactionHistory`.

### Fixed

- [#72](https://github.com/Synesso/scala-stellar-sdk/issues/72) Avoid rounding errors in Ledger responses.

## 0.6.2

### Fixed

- [#65](https://github.com/Synesso/scala-stellar-sdk/issues/65) Restructure `Signer` and `StrKey` and objects to
    accommodate correct serialisation & deserialisation.


## 0.6.1

### Fixed

- [#64](https://github.com/Synesso/scala-stellar-sdk/issues/64) Unsealed ADT traits are now sealed.
- [#65](https://github.com/Synesso/scala-stellar-sdk/issues/65) Failure to parse Set Options Operation JSON responses 
    when the operation set a signer other than an account. This is a partial fix to avoid parse failures.
    More work is required to properly discriminate signer types. This is blocked by required changes to Horizon.

## 0.6.0

### Added

- Federation Server integration. [#5](https://github.com/Synesso/scala-stellar-sdk/issues/5) [#6](https://github.com/Synesso/scala-stellar-sdk/issues/6) 
    - Resolve federated addresses with `KeyPair.fromAddress(address: String): Future[PublicKey]`.
    - Look up the account details by federated name with `FederationServer.byName`.
    - Perform a reverse lookup, where it is supported by the server, with `FederationServer.byAccount`.

- Added `Network.feeStats()` method to return the fee statistics from the most recent ledger, as per the [`fee_stats`
    endpoint](https://www.stellar.org/developers/horizon/reference/endpoints/fee-stats.html).
    [#58](https://github.com/Synesso/scala-stellar-sdk/issues/58)

- Added `Network.info()` method to return the values in the Horizon root document including Horizon & Core versions,
    network passphrase, protocol supported, etc. [#56](https://github.com/Synesso/scala-stellar-sdk/issues/56)

- Included client request headers `X-Client-Name` and `X-Client-Version` so that Horizon instances & Federation Servers
    can tell when a request comes from the Scala SDK. [#59](https://github.com/Synesso/scala-stellar-sdk/issues/59)


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

# Changelog

As this project is pre 1.0, breaking changes may happen for minor version bumps. A breaking change will get clearly notified in this log.

## 0.5.0

### Breaking changes
* Restructuring of transaction submission response types.
    * `TransactionPostResponse` (abstract)
        * `TransactionApproved`
        * `TransactionRejected`

* Complete removal of generated XDR classes in favour of domain objects handling their own XDR encoding.
    This means operations such as `TransactionResult.decodeXDR` will now return instances of domain objects
    newly created in this release which can be used in pattern matches. These classes incorporate all of the 
    XDR encoded information in a user-friendly form and have the following heirarchy:
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

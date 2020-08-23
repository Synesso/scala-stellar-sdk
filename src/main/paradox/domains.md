# Domains

Any domain with an interest in the Stellar network can publish their network information on their
website. This format is defined by [SEP-0001](https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0001.md) and is both human and machine readable.

The SDK can parse all known fields as per v2.0.0 of the specification.

@@snip [DomainInfoItSpec.scala](../../it/scala/stellar/sdk/DomainInfoItSpec.scala) { #domain_info_example }

The domain info spec is rich with data about the organizations that use the Stellar network. 
It is worth bearing in mind that the document is not mandatory and can contain errors. Only fields that
align with the SEP will be parsed.

Continue reading to learn how to use Stellar transactions for @ref:[authentication](authentication.md).

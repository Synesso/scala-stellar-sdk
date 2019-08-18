package stellar.sdk.model.domain

import org.scalacheck.Gen
import stellar.sdk.ArbitraryInput

trait DomainInfoGenerators extends ArbitraryInput {

  val genPointOfContact: Gen[PointOfContact] = for {
    name <- Gen.option(Gen.identifier)
    email <- Gen.option(Gen.identifier)
    keybase <- Gen.option(Gen.identifier)
    telegram <- Gen.option(Gen.identifier)
    twitter <- Gen.option(Gen.identifier)
    github <- Gen.option(Gen.identifier)
    id_photo_hash <- Gen.option(genHash)
    verification_photo_hash <- Gen.option(genHash)
  } yield PointOfContact(
    name, email, keybase, telegram, twitter, github, id_photo_hash, verification_photo_hash)

}

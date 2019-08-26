package stellar.sdk.model.domain

import toml.Value
import toml.Value.Tbl

case class PointOfContact(name: Option[String],
                          email: Option[String],
                          keybase: Option[String],
                          telegram: Option[String],
                          twitter: Option[String],
                          github: Option[String],
                          idPhotoHash: Option[String],
                          verificationPhotoHash: Option[String])

object PointOfContact extends TomlParsers {
  def parse(tbl: Tbl): PointOfContact = {
    def parseTomlValue[T](key: String, parser: PartialFunction[Value, T]) =
      tbl.values.get(key).map(parser.applyOrElse(_, {
        v: Value => throw DomainInfoParseException(s"value for $key was not of the expected type. [value=$v]")
      }))

    PointOfContact(
      name = parseTomlValue("name", string),
      email = parseTomlValue("email", string),
      keybase = parseTomlValue("keybase", string),
      telegram = parseTomlValue("telegram", string),
      twitter = parseTomlValue("twitter", string),
      github = parseTomlValue("github", string),
      idPhotoHash = parseTomlValue("id_photo_hash", string),
      verificationPhotoHash = parseTomlValue("verification_photo_hash", string),
    )
  }
}

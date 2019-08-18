package stellar.sdk.model.domain

import toml.Value
import toml.Value.{Str, Tbl}

case class PointOfContact(name: Option[String],
                          email: Option[String],
                          keybase: Option[String],
                          telegram: Option[String],
                          twitter: Option[String],
                          github: Option[String],
                          idPhotoHash: Option[String],
                          verificationPhotoHash: Option[String])

object PointOfContact {
  def parse(tbl: Tbl): PointOfContact = {
    def parseTomlValue[T](key: String, parser: PartialFunction[Value, T]) =
      tbl.values.get(key).map(parser.applyOrElse(_, {
        v: Value => throw DomainInfoParseException(s"value for $key was not of the expected type. [value=$v]")
      }))

    PointOfContact(
      name = parseTomlValue("name", { case Str(s) => s }),
      email = parseTomlValue("email", { case Str(s) => s }),
      keybase = parseTomlValue("keybase", { case Str(s) => s }),
      telegram = parseTomlValue("telegram", { case Str(s) => s }),
      twitter = parseTomlValue("twitter", { case Str(s) => s }),
      github = parseTomlValue("github", { case Str(s) => s }),
      idPhotoHash = parseTomlValue("id_photo_hash", { case Str(s) => s }),
      verificationPhotoHash = parseTomlValue("verification_photo_hash", { case Str(s) => s }),
    )
  }
}

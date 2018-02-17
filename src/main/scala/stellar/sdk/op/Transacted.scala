package stellar.sdk.op

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk._

case class Transacted[O <: Operation](id: Long,
                                      txnHash: String,
                                      sourceAccount: PublicKeyOps,
                                      createdAt: ZonedDateTime,
                                      operation: O)


object TransactedOperationDeserializer extends CustomSerializer[Transacted[_]](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats + OperationDeserializer
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))
    def account(accountKey: String = "account") = KeyPair.fromAccountId((o \ accountKey).extract[String])
    def date(key: String) = ZonedDateTime.from(formatter.parse((o \ key).extract[String]))

    Transacted(
      id = (o \ "id").extract[String].toLong,
      txnHash = (o \ "transaction_hash").extract[String],
      sourceAccount = account("source_account"),
      createdAt = date("created_at"),
      operation = o.extract[Operation])
}, PartialFunction.empty)
)

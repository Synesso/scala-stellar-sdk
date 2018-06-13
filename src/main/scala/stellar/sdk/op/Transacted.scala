package stellar.sdk.op

import java.time.ZonedDateTime

import org.json4s.JsonAST.JObject
import org.json4s.{CustomSerializer, DefaultFormats}

case class Transacted[O <: Operation](id: Long,
                                      txnHash: String,
                                      createdAt: ZonedDateTime,
                                      operation: O)

object TransactedOperationDeserializer extends CustomSerializer[Transacted[Operation]](format => ( {
  case o: JObject =>
    implicit val formats = DefaultFormats + OperationDeserializer

    def date(key: String) = ZonedDateTime.parse((o \ key).extract[String])

    Transacted(
      id = (o \ "id").extract[String].toLong,
      txnHash = (o \ "transaction_hash").extract[String],
      createdAt = date("created_at"),
      operation = o.extract[Operation])
}, PartialFunction.empty)
)

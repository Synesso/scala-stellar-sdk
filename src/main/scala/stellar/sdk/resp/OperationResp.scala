package stellar.sdk.resp

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import org.json4s.JsonAST.{JArray, JObject, JValue}
import org.json4s.{CustomSerializer, DefaultFormats}
import stellar.sdk._

sealed trait OperationResp {
  val id: Long
  val txnHash: String
  val sourceAccount: PublicKeyOps
  val createdAt: ZonedDateTime
}


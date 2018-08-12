package stellar.sdk.inet


sealed trait HorizonCursor
case object Now extends HorizonCursor
case class Record(value: Long) extends HorizonCursor

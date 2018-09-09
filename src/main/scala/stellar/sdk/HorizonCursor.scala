package stellar.sdk

sealed trait HorizonCursor {
  def paramValue: String
}

case object Now extends HorizonCursor {
  override def paramValue: String = "now"
}

case class Record(value: Long) extends HorizonCursor {
  override def paramValue: String = s"$value"
}

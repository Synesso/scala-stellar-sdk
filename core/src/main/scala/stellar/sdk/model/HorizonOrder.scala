package stellar.sdk.model

sealed trait HorizonOrder {
  def paramValue: String
}

case object Asc extends HorizonOrder {
  override def paramValue: String = "asc"
}

case object Desc extends HorizonOrder {
  override def paramValue: String = "desc"
}

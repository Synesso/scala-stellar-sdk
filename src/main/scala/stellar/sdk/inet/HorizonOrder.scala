package stellar.sdk.inet


sealed trait HorizonOrder {
  def paramValue: String
}

case object Asc extends HorizonOrder {
  def paramValue: String = "asc"
}

case object Desc extends HorizonOrder {
  def paramValue: String = "desc"
}

package stellar.sdk.inet


sealed trait HorizonOrder
case object Asc extends HorizonOrder
case object Desc extends HorizonOrder

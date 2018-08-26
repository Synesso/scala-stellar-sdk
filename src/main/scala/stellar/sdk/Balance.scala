package stellar.sdk

case class Balance(amount: Amount, limit: Option[Long] = None, buyingLiabilities: Long = 0, sellingLiabilities: Long = 0)

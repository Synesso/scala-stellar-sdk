package stellar.sdk.model

import stellar.sdk.PublicKey

case class Balance(
  amount: Amount,
  limit: Option[Long] = None,
  buyingLiabilities: Long = 0,
  sellingLiabilities: Long = 0,
  authorized: Boolean = false,
  authorizedToMaintainLiabilities: Boolean = false,
  sponsor: Option[PublicKey] = None
)

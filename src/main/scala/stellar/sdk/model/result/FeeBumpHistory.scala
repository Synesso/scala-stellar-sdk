package stellar.sdk.model.result

import stellar.sdk.model.NativeAmount

case class FeeBumpHistory(maxFee: NativeAmount, hash: String, signatures: List[String])
package stellar.sdk.model

import stellar.sdk.Signature

case class FeeBump(source: Account, fee: NativeAmount, signatures: List[Signature])
package stellar.sdk.model

import stellar.sdk.Signature

case class FeeBump(source: AccountId, fee: NativeAmount, signatures: List[Signature])
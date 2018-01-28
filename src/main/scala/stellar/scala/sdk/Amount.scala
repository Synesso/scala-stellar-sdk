package stellar.scala.sdk

trait Amount {
  val units: Long
  val asset: Asset
  def toHumanValue: Double = units / math.pow(10, Amount.decimalPlaces)
}

case class NativeAmount(units: Long) extends Amount {
  override val asset: Asset = AssetTypeNative
}

case class IssuedAmount(units: Long, asset: NonNativeAsset) extends Amount

object Amount {
  private val decimalPlaces = 7

  def apply(units: Long, asset: Asset): Amount = {
    asset match {
      case AssetTypeNative => NativeAmount(units)
      case a: NonNativeAsset => IssuedAmount(units, a)
    }
  }
  def lumens(units: Double): NativeAmount = {
    NativeAmount((units * math.pow(10, decimalPlaces)).floor.toLong)
  }
}

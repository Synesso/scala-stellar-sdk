package stellar.scala.sdk

case class Amount(stroops: Long) extends AnyVal {
  def toLumen: Double = stroops / math.pow(10, 7)
}

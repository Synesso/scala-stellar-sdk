package stellar.scala.sdk

trait ByteArrays {

  def paddedByteArray(bs: Array[Byte], length: Int): Array[Byte] = {
    val padded = Array.ofDim[Byte](length)
    System.arraycopy(bs, 0, padded, 0, bs.length)
    padded
  }

  def paddedByteArray(s: String, length: Int): Array[Byte] = paddedByteArray(s.getBytes, length)

}

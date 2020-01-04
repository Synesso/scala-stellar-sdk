package stellar.sdk.key

import okio.ByteString
import org.specs2.mutable.Specification

class HDNodeSpec extends Specification {

  "HD master node" should {
    "be derived from entropy" >> {
      // https://github.com/satoshilabs/slips/blob/master/slip-0010.md#test-vector-1-for-ed25519
      val masterNode = HDNode.fromEntropy(ByteString.decodeHex("000102030405060708090a0b0c0d0e0f"))
      masterNode.privateKey mustEqual ByteString.decodeHex("2b4be7f19ee27bbf30c667b642d5f4aa69fd169872f8fc3059c08ebae2eb19e7")
      masterNode.chainCode mustEqual ByteString.decodeHex("90046a93de5380a72b5e45010748567d5ea02bbf6522f979e05c0d8d8ca9fffb")
    }

    "deterministically derive child nodes" >> {
      val masterNode = HDNode.fromEntropy(ByteString.decodeHex("000102030405060708090a0b0c0d0e0f"))
      val childZero = masterNode.deriveChild(0)
      val childOne = childZero.deriveChild(1)

      childZero.privateKey mustEqual ByteString.decodeHex("68e0fe46dfb67e368c75379acec591dad19df3cde26e63b93a8e704f1dade7a3")
      childZero.chainCode mustEqual ByteString.decodeHex("8b59aa11380b624e81507a27fedda59fea6d0b779a778918a2fd3590e16e9c69")

      childOne.privateKey mustEqual ByteString.decodeHex("b1d0bad404bf35da785a64ca1ac54b2617211d2777696fbffaf208f746ae84f2")
      childOne.chainCode mustEqual ByteString.decodeHex("a320425f77d1b5c2505a6b1b27382b37368ee640e3557c315416801243552f14")
    }
  }
}

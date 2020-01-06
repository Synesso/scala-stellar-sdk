package stellar.sdk.key

import java.nio.charset.StandardCharsets.UTF_8
import java.security.SecureRandom

import io.github.novacrypto.bip39.{MnemonicGenerator, SeedCalculator}
import okio.ByteString
import stellar.sdk.KeyPair
import stellar.sdk.util.ByteArrays.Implicits._

import scala.collection.mutable

case class Mnemonic(phrase: List[String], wordList: WordList = EnglishWords) {
  require(phrase.size % 3 == 0, "Invalid mnemonic phrase. Length must be a multiple of 3.")
  require(phrase.forall(wordList.contains), "Invalid mnemonic phrase. It includes words not in the word list. " +
    s"[invalidWords=${phrase.filterNot(wordList.contains)}][wordList=${wordList.getClass.getSimpleName}]")

  def validateChecksum(): Unit = entropy

  def entropy: ByteString = {

    val bitString: String = phrase.flatMap(wordList.indexOf).map(toBitString(_, 11)).mkString

    val numChecksumBits = bitString.length / 33
    val numEntropyBits = bitString.length - numChecksumBits

    val checksumBits = bitString.drop(numEntropyBits)

    val entropyBits = bitString.take(numEntropyBits)
    val entropyHex = parseBitString(entropyBits)

    val entropyBytes = new ByteString(entropyHex.array)

    val checksumProvided = new ByteString(parseBitString(checksumBits).array)
    val checksumDerived = deriveChecksum(entropyBytes)

    assert(checksumDerived == checksumProvided,
      s"Checksums did not match. [provided=${checksumProvided}][derived=${checksumDerived}]")

    entropyBytes
  }

  /**
    * @return The root hierarchical deterministic node that this mnemonic maps to.
    */
  def asHDNode(): HDNode = asHDNode(new ByteString(Array.emptyByteArray))

  /**
    * @param passphrase the password to apply to the HD node mapping.
    * @return The root hierarchical deterministic node that this mnemonic maps to.
    */
  def asHDNode(passphrase: ByteString): HDNode = {
    validateChecksum()
    val seed = new SeedCalculator().calculateSeed(phrase.mkString(" "), passphrase.string(UTF_8))
    HDNode.fromEntropy(seed)
  }

  /**
    * @return The keypair attached to the root hierarchical deterministic node that this mnemonic
    *         maps to.
    */
  def asRootKeyPair(): KeyPair = asHDNode().deriveChild(44, 148, 0).asKeyPair

  /**
    * @param passphrase the password to apply to the HD node mapping.
    * @return The keypair attached to the root hierarchical deterministic node that this mnemonic
    *         maps to.
    */
  def asRootKeyPair(passphrase: ByteString): KeyPair =
    asHDNode(passphrase).deriveChild(44, 148, 0).asKeyPair

  def phraseString: String = phrase.mkString(wordList.separator)

  private def deriveChecksum(entropyBytes: ByteString): ByteString = {
    val checksumLengthBits = (entropyBytes.size * 8) / 32
    val hashBytes = entropyBytes.sha256().toByteArray
    val checksumLengthBytes = math.ceil(checksumLengthBits / 8.0).toInt

    val reducedBytesToChecksum = hashBytes.array.take(checksumLengthBytes)
    val reducedChecksumBits = reducedBytesToChecksum.map(toBitString).mkString
    val checksumBitString = reducedChecksumBits.take(checksumLengthBits)

    new ByteString(parseBitString(checksumBitString).array)
  }

  private def toBitString(b: Byte): String = toBitString(b.toInt & 0xff, 8)

  private def toBitString(i: Int, size: Int): String =
    String.format("%1$" + size + "s", Integer.toBinaryString(i)).replace(' ', '0')

  private def parseBitString(bitString: String): mutable.WrappedArray[Byte] = {
    val resizedBitString = "0" * math.max(0, 8 - bitString.length) + bitString
    resizedBitString.sliding(8, 8)
      .map(Integer.parseInt(_, 2).toByte)
      .toArray
  }
}

object Mnemonic {
  /**
    * Returns a mnemonic phrase from the given entropy.
    * @param wordList the list of words to build the phrase from.
    * @param entropy the provided randomness behind the mnemonic phrase generation.
    * @return a mnemonic phrase derived from the wordlist and entropy.
    */
  def fromEntropy(wordList: WordList = EnglishWords, entropy: Array[Byte]): Mnemonic = {
    val words = mutable.Buffer.empty[String]
    val internalList = new io.github.novacrypto.bip39.WordList() {
      override def getWord(index: Int): String = wordList.wordAt(index)
      override def getSpace: Char = wordList.separator.head
    }
    new MnemonicGenerator(internalList).createMnemonic(entropy, { word =>
      if (word != wordList.separator) words.append(word.toString)
    })
    Mnemonic(words.toList, wordList)
  }

  /**
    * Returns a random mnemonic phrase with the given size of entropy, using software randomness.
    * @param wordList the list of words to build the phrase from.
    * @param entropyBits the number of bits of randomness behind the mnemonic phrase generation.
    * @return a random mnemonic phrase from the wordlist matching the quantity of entropy bits.
    */
  def random(wordList: WordList = EnglishWords, entropyBits: Int = 256): Mnemonic = {
    require(entropyBits % 32 == 0 && entropyBits >= 128 && entropyBits <= 256,
      "Provided entropyBits must be between 128 and 256 inclusive. It must be divisible by 32.")
    val entropy = Array.ofDim[Byte](entropyBits / 8)
    new SecureRandom().nextBytes(entropy)
    fromEntropy(wordList, entropy)
  }
}
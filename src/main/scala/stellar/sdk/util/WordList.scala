package stellar.sdk.util

import io.github.novacrypto.bip39.wordlists._

trait WordList {
  def word(i: Int): String
}

protected class InternalWordList(internalWordList: io.github.novacrypto.bip39.WordList) extends WordList {
  override def word(i: Int): String = internalWordList.getWord(i)
}

object EnglishWords extends InternalWordList(English.INSTANCE)
object FrenchWords extends InternalWordList(French.INSTANCE)
object JapaneseWords extends InternalWordList(Japanese.INSTANCE)
object SpanishWords extends InternalWordList(Spanish.INSTANCE)
package stellar.sdk.key

import scala.io.Source

trait WordList {
  def indexOf(word: String): Option[Int]
  def wordAt(i: Int): String
  def contains(word: String): Boolean = indexOf(word).isDefined
  def separator: String
}

class ArrayBackedWordList(source: => Source, val separator: String = " ") extends WordList {
  lazy val words: Array[String] = source.getLines().toArray

  // TODO (jem) - WordList spec that ensures index can be found with normalized variants.
  override def indexOf(word: String): Option[Int] = Some(words.indexOf(word)).filter(_ >= 0)

  override def wordAt(i: Int): String = {
    require(i >= 0 && i < words.length, s"Word index $i is out of range.")
    words(i)
  }
}

object ChineseSimplifiedWords extends ArrayBackedWordList(Source.fromResource("wordlists/chinese_simplified.txt"))
object ChineseTraditionalWords extends ArrayBackedWordList(Source.fromResource("wordlists/chinese_traditional.txt"))
object CzechWords extends ArrayBackedWordList(Source.fromResource("wordlists/czech.txt"))
object EnglishWords extends ArrayBackedWordList(Source.fromResource("wordlists/english.txt"))
object FrenchWords extends ArrayBackedWordList(Source.fromResource("wordlists/french.txt"))
object ItalianWords extends ArrayBackedWordList(Source.fromResource("wordlists/italian.txt"))
object JapaneseWords extends ArrayBackedWordList(Source.fromResource("wordlists/japanese.txt"), "\u3000")
object KoreanWords extends ArrayBackedWordList(Source.fromResource("wordlists/korean.txt"))
object SpanishWords extends ArrayBackedWordList(Source.fromResource("wordlists/spanish.txt"))

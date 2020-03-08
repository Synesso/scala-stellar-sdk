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

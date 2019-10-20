package stellar.sdk.key

trait WordList {
  def indexOf(word: String): Option[Int]
  def wordAt(i: Int): String
  def contains(word: String): Boolean = indexOf(word).isDefined
  def separator: String
}

abstract class ArrayBackedWordList extends WordList {
  val words: Array[String]

  // TODO (jem) - WordList spec that ensures index can be found with normalized variants.
  override def indexOf(word: String): Option[Int] = Some(words.indexOf(word)).filter(_ >= 0)

  override def wordAt(i: Int): String = {
    require(i >= 0 && i < words.length, s"Word index $i is out of range.")
    words(i)
  }

  override def separator: String = " "
}

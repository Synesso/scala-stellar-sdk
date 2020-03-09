package stellar.sdk.key

import org.specs2.mutable.Specification

class WordListSpec extends Specification {

  val supported = List(
    ChineseSimplifiedWords,
    ChineseTraditionalWords,
    CzechWords,
    EnglishWords,
    FrenchWords,
    ItalianWords,
    JapaneseWords,
    KoreanWords,
    SpanishWords
  )

  "a wordlist" should {
    "allow indexed access to all words" >> {
      forall(supported) { wordList =>
        wordList.words.length mustEqual 2048
        wordList.words.distinct.length mustEqual 2048
        forall(wordList.words) { word =>
          wordList.indexOf(word).map(wordList.wordAt) must beSome(word)
        }
      }
    }

    "disallow access to words outside the index" >> {
      forall(supported) { wordList =>
        wordList.wordAt(-1) must throwAn[IllegalArgumentException]
        wordList.wordAt(wordList.words.length) must throwAn[IllegalArgumentException]
      }
    }
  }
}

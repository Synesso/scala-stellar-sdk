package stellar.sdk.key

import org.specs2.mutable.Specification

class WordListSpec extends Specification {

  "a wordlist" should {
    "allow indexed access to all words" >> {
      forall(List(EnglishWords, FrenchWords, JapaneseWords, SpanishWords)) { wordList =>
        wordList.words.length mustEqual 2048
        wordList.words.distinct.length mustEqual 2048
        forall(wordList.words) { word =>
          wordList.indexOf(word).map(wordList.wordAt) must beSome(word)
        }
      }
    }

    "disallow access to words outside the index" >> {
      forall(List(EnglishWords, FrenchWords, JapaneseWords, SpanishWords)) { wordList =>
        wordList.wordAt(-1) must throwAn[IllegalArgumentException]
        wordList.wordAt(wordList.words.length) must throwAn[IllegalArgumentException]
      }
    }
  }
}

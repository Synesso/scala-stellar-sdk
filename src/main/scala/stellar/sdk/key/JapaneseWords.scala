package stellar.sdk.key

import scala.io.Source

object JapaneseWords extends ArrayBackedWordList(Source.fromResource("wordlists/japanese.txt"), "\u3000")


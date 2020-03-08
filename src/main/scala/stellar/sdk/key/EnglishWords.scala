package stellar.sdk.key

import scala.io.Source

object EnglishWords extends ArrayBackedWordList(Source.fromResource("wordlists/english.txt"))

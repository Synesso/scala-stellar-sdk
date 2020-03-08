package stellar.sdk.key

import scala.io.Source

object FrenchWords extends ArrayBackedWordList(Source.fromResource("wordlists/french.txt"))

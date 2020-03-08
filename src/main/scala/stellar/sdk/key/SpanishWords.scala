package stellar.sdk.key

import scala.io.Source

object SpanishWords extends ArrayBackedWordList(Source.fromResource("wordlists/spanish.txt"))

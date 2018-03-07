package stellar.sdk.resp

import org.specs2.mutable.Specification
import stellar.sdk.ArbitraryInput
import stellar.sdk.op.JsonSnippets

class TransactionRespSpec extends Specification with ArbitraryInput with JsonSnippets {


}

/*
{
"_links": {
  "transaction": {
    "href": "https://horizon-testnet.stellar.org/transactions/ba68c0112afe25a2fea9a6e7926a4aef9ff12fb627ec840840541813aaa695db"
  }
},
"hash": "ba68c0112afe25a2fea9a6e7926a4aef9ff12fb627ec840840541813aaa695db",
"ledger": 7729219,
"envelope_xdr": "AAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAZAB16IkAAAABAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAuRsw+AoWiSHa1TWuxE8O0ve5Ytj2JJE1sDrLNJspsxsAAAAAAJiWgAAAAAAAAAABDJm32AAAAEDnDn8POBeTu0v5Hj6VCVBKABHtap9ut+HH0+taBQsDPNLA+WXfiwrq1hG5cEQP0qTHG59vkmyjxcejqjz7dPwO",
"result_xdr": "AAAAAAAAAGQAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAA=",
"result_meta_xdr": "AAAAAAAAAAEAAAADAAAAAAB18EMAAAAAAAAAALkbMPgKFokh2tU1rsRPDtL3uWLY9iSRNbA6yzSbKbMbAAAAAACYloAAdfBDAAAAAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAwB18EMAAAAAAAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAF0h255wAdeiJAAAAAQAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAQB18EMAAAAAAAAAAJYHU4BtUa8ACOZZzHII4+FtEgRa9lBknmI+jQ8MmbfYAAAAF0feURwAdeiJAAAAAQAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAA"
}


    val is = new XdrDataInputStream(new ByteArrayInputStream(baos.toByteArray))
    XDRMemo.decode(is) must beEquivalentTo(xdrMemo)


 */

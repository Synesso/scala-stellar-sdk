package stellar.sdk

import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.apache.commons.codec.binary.Hex
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.{Specification, Tables}
import stellar.sdk.key.JapaneseWords
import stellar.sdk.util.ByteArrays
// #enable-implicit-byte-array-conversion
import stellar.sdk.util.ByteArrays.Implicits._
// #enable-implicit-byte-array-conversion

import scala.concurrent.Future
import scala.concurrent.duration._

class KeyPairSpec(implicit ee: ExecutionEnv) extends Specification
  with Tables with ArbitraryInput with DomainMatchers {

  private val keyPair = KeyPair.fromSecretSeed(
    Hex.decodeHex("1123740522f11bfef6b3671f51e159ccf589ccf8965262dd5f97d1721d383dd4")
  )
  private val sig = "587d4b472eeef7d07aafcd0b049640b0bb3f39784118c2e2b73a04fa2f64c9c538b4b2d0f5335e968a480021fdc23e98c0ddf424cb15d8131df8cb6c4bb58309"

  "signed data" should {
    "be verified by the signing key" >> prop { msg: String =>
      keyPair.verify(msg.getBytes("UTF-8"), keyPair.sign(msg.getBytes("UTF-8")).data) must beTrue
    }

    "be correct for concrete example" >> {
      val data = "hello world"
      val actual = keyPair.sign(data.getBytes("UTF-8")).data
      Hex.encodeHex(actual).mkString mustEqual sig
    }

    "verify true for a concrete example of a valid signature" >> {
      val data = "hello world"
      keyPair.verify(data.getBytes("UTF-8"), Hex.decodeHex(sig)) must beTrue
    }

    "verify false for a concrete example of an invalid signature" >> {
      val data = "今日は世界"
      keyPair.verify(data.getBytes("UTF-8"), Hex.decodeHex(sig)) must beFalse
    }

    "verify false for random rubbish" >> prop { msg: String =>
      keyPair.verify(msg.getBytes("UTF-8"), msg.getBytes("UTF-8")) must beFalse
    }
  }

  "a key pair" should {
    "report its account id and secret seed and be reconstituted from these" >> prop { kp: KeyPair =>
      kp.accountId.toCharArray must haveLength(56)
      kp.accountId must startWith("G")
      KeyPair.fromPublicKey(kp.publicKey) must beEquivalentTo(kp.asPublicKey)
      KeyPair.fromSecretSeed(kp.secretSeed) must beEquivalentTo(kp)
      KeyPair.fromSecretSeed(kp.secretSeed.mkString) must beEquivalentTo(kp)
      KeyPair.fromAccountId(kp.accountId) must beEquivalentTo(kp.asPublicKey)
    }

    "not be constructed from an invalid account id" >> {
      val badId = "GACZHAQLFECAHDSFDQPCOAD6ITVWR7BUZAIRRUGOAPLECX74O6223A4G"
      KeyPair.fromAccountId(badId) must throwA[InvalidAccountId].like {
        case e: InvalidAccountId => e.getMessage mustEqual badId
      }
    }

    "not be constructed from an invalid secret seed" >> {
      val nickCave = ""
      KeyPair.fromSecretSeed(nickCave) must throwAn[InvalidSecretSeed]
    }

/*
    "have a mnemonic" >> prop { (kp: KeyPair, wordList: WordList) =>
      val phrase = kp.mnemonic(wordList)
      phrase must haveSize(24)
      phrase must contain(not(beEmpty[String])).forall
      KeyPair.fromMnemonic(phrase, wordList) must beEquivalentTo(kp)
    }
*/

    "be constructed from a 12-word mnemonic phrase" >> {
      "index" | "secret"                                                   |>
      "0"     ! "SBGWSG6BTNCKCOB3DIFBGCVMUPQFYPA2G4O34RMTB343OYPXU5DJDVMN" |
      "1"     ! "SCEPFFWGAG5P2VX5DHIYK3XEMZYLTYWIPWYEKXFHSK25RVMIUNJ7CTIS" |
      "2"     ! "SDAILLEZCSA67DUEP3XUPZJ7NYG7KGVRM46XA7K5QWWUIGADUZCZWTJP" |
      "3"     ! "SBMWLNV75BPI2VB4G27RWOMABVRTSSF7352CCYGVELZDSHCXWCYFKXIX" |
      "4"     ! "SCPCY3CEHMOP2TADSV2ERNNZBNHBGP4V32VGOORIEV6QJLXD5NMCJUXI" |
      "5"     ! "SCK27SFHI3WUDOEMJREV7ZJQG34SCBR6YWCE6OLEXUS2VVYTSNGCRS6X" |
      "6"     ! "SDJ4WDPOQAJYR3YIAJOJP3E6E4BMRB7VZ4QAEGCP7EYVDW6NQD3LRJMZ" |
      "7"     ! "SA3HXJUCE2N27TBIZ5JRBLEBF3TLPQEBINP47E6BTMIWW2RJ5UKR2B3L" |
      "8"     ! "SCD5OSHUUC75MSJG44BAT3HFZL2HZMMQ5M4GPDL7KA6HJHV3FLMUJAME" |
      "9"     ! "SCJGVMJ66WAUHQHNLMWDFGY2E72QKSI3XGSBYV6BANDFUFE7VY4XNXXR" | { (i, sec) =>
        KeyPair.fromMnemonicPhrase("illness spike retreat truth genius clock brain pass fit " +
          "cave bargain toe", index = i.toInt) must beEquivalentTo(KeyPair.fromSecretSeed(sec))
      }
    }

    "be constructed from a 15-word mnemonic phrase" >> {
      "index" | "secret"                                                   |>
      "0"     ! "SAKS7I2PNDBE5SJSUSU2XLJ7K5XJ3V3K4UDFAHMSBQYPOKE247VHAGDB" |
      "1"     ! "SAZ2H5GLAVWCUWNPQMB6I3OHRI63T2ACUUAWSH7NAGYYPXGIOPLPW3Q4" |
      "2"     ! "SDVSSLPL76I33DKAI4LFTOAKCHJNCXUERGPCMVFT655Z4GRLWM6ZZTSC" |
      "3"     ! "SCH56YSGOBYVBC6DO3ZI2PY62GBVXT4SEJSXJOBQYGC2GCEZSB5PEVBZ" |
      "4"     ! "SBWBM73VUNBGBMFD4E2BA7Q756AKVEAAVTQH34RYEUFD6X64VYL5KXQ2" |
      "5"     ! "SAVS4CDQZI6PSA5DPCC42S5WLKYIPKXPCJSFYY4N3VDK25T2XX2BTGVX" |
      "6"     ! "SDFC7WZT3GDQVQUQMXN7TC7UWDW5E3GSMFPHUT2TSTQ7RKWTRA4PLBAL" |
      "7"     ! "SA6UO2FIYC6AS2MSDECLR6F7NKCJTG67F7R4LV2GYB4HCZYXJZRLPOBB" |
      "8"     ! "SBDNHDDICLLMBIDZ2IF2D3LH44OVUGGAVHQVQ6BZQI5IQO6AB6KNJCOV" |
      "9"     ! "SDHRG2J34MGDAYHMOVKVJC6LX2QZMCTIKRO5I4JQ6BJQ36KVL6QUTT72" | { (i, sec) =>
        KeyPair.fromMnemonicPhrase("resource asthma orphan phone ice canvas fire useful arch " +
          "jewel impose vague theory cushion top", index = i.toInt) must beEquivalentTo(
          KeyPair.fromSecretSeed(sec))
      }
    }

    "be constructed from a 24-word mnemonic phrase" >> {
      "index" | "secret"                                                   |>
      "0"     ! "SAEWIVK3VLNEJ3WEJRZXQGDAS5NVG2BYSYDFRSH4GKVTS5RXNVED5AX7" |
      "1"     ! "SBKSABCPDWXDFSZISAVJ5XKVIEWV4M5O3KBRRLSPY3COQI7ZP423FYB4" |
      "2"     ! "SD5CCQAFRIPB3BWBHQYQ5SC66IB2AVMFNWWPBYGSUXVRZNCIRJ7IHESQ" |
      "3"     ! "SBSGSAIKEF7JYQWQSGXKB4SRHNSKDXTEI33WZDRR6UHYQCQ5I6ZGZQPK" |
      "4"     ! "SBIZH53PIRFTPI73JG7QYA3YAINOAT2XMNAUARB3QOWWVZVBAROHGXWM" |
      "5"     ! "SCVM6ZNVRUOP4NMCMMKLTVBEMAF2THIOMHPYSSMPCD2ZU7VDPARQQ6OY" |
      "6"     ! "SBSHUZQNC45IAIRSAHMWJEJ35RY7YNW6SMOEBZHTMMG64NKV7Y52ZEO2" |
      "7"     ! "SC2QO2K2B4EBNBJMBZIKOYSHEX4EZAZNIF4UNLH63AQYV6BE7SMYWC6E" |
      "8"     ! "SCGMC5AHAAVB3D4JXQPCORWW37T44XJZUNPEMLRW6DCOEARY3H5MAQST" |
      "9"     ! "SCPA5OX4EYINOPAUEQCPY6TJMYICUS5M7TVXYKWXR3G5ZRAJXY3C37GF" | { (i, sec) =>
        KeyPair.fromMnemonicPhrase("bench hurt jump file august wise shallow faculty impulse " +
          "spring exact slush thunder author capable act festival slice deposit sauce coconut " +
          "afford frown better", index = i.toInt) must beEquivalentTo(
          KeyPair.fromSecretSeed(sec))
      }
    }

    "be constructed from a 24-word mnemonic phrase with passphrase" >> {
      "index" | "secret"                                                   |>
      "0"     ! "SAFWTGXVS7ELMNCXELFWCFZOPMHUZ5LXNBGUVRCY3FHLFPXK4QPXYP2X" |
      "1"     ! "SBQPDFUGLMWJYEYXFRM5TQX3AX2BR47WKI4FDS7EJQUSEUUVY72MZPJF" |
      "2"     ! "SAF2LXRW6FOSVQNC4HHIIDURZL4SCGCG7UEGG23ZQG6Q2DKIGMPZV6BZ" |
      "3"     ! "SDCCVBIYZDMXOR4VPC3IYMIPODNEDZCS44LDN7B5ZWECIE57N3BTV4GQ" |
      "4"     ! "SA5TRXTO7BG2Z6QTQT3O2LC7A7DLZZ2RBTGUNCTG346PLVSSHXPNDVNT" |
      "5"     ! "SDEOED2KPHV355YNOLLDLVQB7HDPQVIGKXCAJMA3HTM4325ZHFZSKKUC" |
      "6"     ! "SDYNO6TLFNV3IM6THLNGUG5FII4ET2H7NH3KCT6OAHIUSHKR4XBEEI6A" |
      "7"     ! "SDXMJXAY45W3WEFWMYEPLPIF4CXAD5ECQ37XKMGY5EKLM472SSRJXCYD" |
      "8"     ! "SAIZA26BUP55TDCJ4U7I2MSQEAJDPDSZSBKBPWQTD5OQZQSJAGNN2IQB" |
      "9"     ! "SDXDYPDNRMGOF25AWYYKPHFAD3M54IT7LCLG7RWTGR3TS32A4HTUXNOS" | { (i, sec) =>
        KeyPair.fromMnemonicPhrase(
          phrase = "cable spray genius state float twenty onion head street palace net private " +
            "method loan turn phrase state blanket interest dry amazing dress blast tube",
          passphrase = "p4ssphr4se".getBytes("UTF-8"),
          index = i.toInt) must beEquivalentTo(
          KeyPair.fromSecretSeed(sec))
      }
    }

    "an example of constructing from a 24-word mnemonic phrase with a passphrase" >> {
      // #keypair-from-mnemonic
      val keyPair = KeyPair.fromMnemonicPhrase(
        phrase = "cable spray genius state float twenty onion head street palace net private " +
          "method loan turn phrase state blanket interest dry amazing dress blast tube",
        passphrase = "p4ssphr4se".getBytes("UTF-8"))
      keyPair.accountId mustEqual "GDAHPZ2NSYIIHZXM56Y36SBVTV5QKFIZGYMMBHOU53ETUSWTP62B63EQ"
      // #keypair-from-mnemonic
    }

    "an example of constructing from a 24-word mnemonic phrase in Japanese" >> {
      // #keypair-from-mnemonic-japanese
      val keyPair = KeyPair.fromMnemonicPhrase(
        phrase = "つぶす　きそう　かるい　ようじ　なまいき　むさぼる　あこがれる　そっせん　みせる　しちょう　" +
          "そんしつ　まろやか　しへい　さわやか　でんあつ　めした　せんとう　だっきゃく　ほっさ　ひるやすみ　" +
          "はさみ　ようちえん　おんだん　えらい",
        wordList = JapaneseWords)
      keyPair.accountId mustEqual "GDEIRKSGFKJCCXUQAM2KVUOAFV626NESTZ5Q4FRCUXTTYHK6RTN66TY2"
      // #keypair-from-mnemonic-japanese
    }
  }

  "a public key" should {
    "have a hashcode equal to the account id" >> prop { pk: PublicKey =>
      pk.hashCode mustEqual pk.accountId.hashCode
    }

    "be equal to the keypair it originated from" >> prop { kp: KeyPair =>
      kp.asPublicKey mustEqual kp
    }

    "not be equal to non-PublicKeyOps instances" >> prop { pk: PublicKey =>
      pk must not(beEqualTo(pk.accountId))
    }

    "be constructable from the internal 'a-byte'" >> prop { pk: PublicKey =>
      pk must beEqualTo(KeyPair.fromPublicKey(pk.publicKey))
    }

    "be constructable from a passphrase" >> {
      // #keypair_from_passphrase
      val kp = KeyPair.fromPassphrase(
        "But, the Babel fish is a dead giveaway isn't it?"
      )
      // #keypair_from_passphrase
      kp mustEqual KeyPair.fromSecretSeed("SDHJNFV6MEPGT2FTAADH2ACHHXIV72F4VV4Q3WLYOKZTK7XB62NAOZPA")
    }

    "serde via xdr bytes" >> prop { pk: PublicKey =>
      KeyPair.decode(pk.xdr) must beEquivalentTo(pk)
    }
  }

  "a federated address" should {
    "resolved to a keypair when it exists" >> {
      // #keypair_from_federated_address
      val resolved: Future[PublicKey] = KeyPair.fromAddress("jem*keybase.io")
      // #keypair_from_federated_address
      resolved must beEqualTo(
        KeyPair.fromAccountId("GBRAZP7U3SPHZ2FWOJLHPBO3XABZLKHNF6V5PUIJEEK6JEBKGXWD2IIE")
      ).awaitFor(1.minute)
    }

    "fail when the name does not exist" >> {
      KeyPair.fromAddress("asodifuawehksdjhlsduyfasdjfh*stronghold.co") must throwA[NoSuchAddress].awaitFor(1.minute)
    }

    "fail when the well-known.toml doesn't contain a federation server" >> {
      val server = new MockWebServer()
      server.enqueue(new MockResponse().setBody("FOO=123"))
      server.start()
      val response = KeyPair.fromAddress("abc*localhost:8002") must throwA[NoSuchAddress].awaitFor(1.minute)
      server.shutdown()
      response
    }

    "fail when the domain does not resolve" >> {
      KeyPair.fromAddress("jem*no.such.top.level.domain") must throwA[NoSuchAddress].awaitFor(1.minute)
    }

    "fail when the address is not in the correct format" >> {
      KeyPair.fromAddress("no asterisk") must throwA[NoSuchAddress].awaitFor(1.minute)
    }
  }
}

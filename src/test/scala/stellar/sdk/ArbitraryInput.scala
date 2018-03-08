package stellar.sdk

import java.time.temporal.ChronoField
import java.time.{Instant, ZoneId, ZonedDateTime}

import org.apache.commons.codec.binary.Base64
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.stellar.sdk.xdr.{Operation => _, _}
import stellar.sdk.op._
import stellar.sdk.resp._

import scala.util.Random

trait ArbitraryInput extends ScalaCheck {

  implicit val network: Network = TestNetwork

  implicit def arbKeyPair: Arbitrary[KeyPair] = Arbitrary(genKeyPair)

  implicit def arbPublicKey: Arbitrary[PublicKey] = Arbitrary(genPublicKey)

  implicit def arbAccount: Arbitrary[Account] = Arbitrary(genAccount)

  implicit def arbAmount: Arbitrary[Amount] = Arbitrary(genAmount)

  implicit def arbNativeAmount: Arbitrary[NativeAmount] = Arbitrary(genNativeAmount)

  implicit def arbIssuedAmount: Arbitrary[IssuedAmount] = Arbitrary(genIssuedAmount)

  implicit def arbAsset: Arbitrary[Asset] = Arbitrary(genAsset)

  implicit def arbNonNativeAsset: Arbitrary[NonNativeAsset] = Arbitrary(genNonNativeAsset)

  implicit def arbAccountMergeOperation: Arbitrary[AccountMergeOperation] = Arbitrary(genAccountMergeOperation)

  implicit def arbAllowTrustOperation: Arbitrary[AllowTrustOperation] = Arbitrary(genAllowTrustOperation)

  implicit def arbChangeTrustOperation: Arbitrary[ChangeTrustOperation] = Arbitrary(genChangeTrustOperation)

  implicit def arbCreateAccountOperation: Arbitrary[CreateAccountOperation] = Arbitrary(genCreateAccountOperation)

  implicit def arbCreatePassiveOfferOperation: Arbitrary[CreatePassiveOfferOperation] = Arbitrary(genCreatePassiveOfferOperation)

  implicit def arbWriteDataOperation: Arbitrary[WriteDataOperation] = Arbitrary(genWriteDataOperation)

  implicit def arbDeleteDataOperation: Arbitrary[DeleteDataOperation] = Arbitrary(genDeleteDataOperation)

  implicit def arbCreateOfferOperation: Arbitrary[CreateOfferOperation] = Arbitrary(genCreateOfferOperation)

  implicit def arbDeleteOfferOperation: Arbitrary[DeleteOfferOperation] = Arbitrary(genDeleteOfferOperation)

  implicit def arbUpdateOfferOperation: Arbitrary[UpdateOfferOperation] = Arbitrary(genUpdateOfferOperation)

  implicit def arbPathPaymentOperation: Arbitrary[PathPaymentOperation] = Arbitrary(genPathPaymentOperation)

  implicit def arbPaymentOperation: Arbitrary[PaymentOperation] = Arbitrary(genPaymentOperation)

  implicit def arbSetOptionsOperation: Arbitrary[SetOptionsOperation] = Arbitrary(genSetOptionsOperation)

  implicit def arbPrice: Arbitrary[Price] = Arbitrary(genPrice)

  implicit def arbOperation: Arbitrary[Operation] = Arbitrary(genOperation)

  implicit def arbInstant: Arbitrary[Instant] = Arbitrary(genInstant)

  implicit def arbTimeBounds: Arbitrary[TimeBounds] = Arbitrary(genTimeBounds)

  implicit def arbMemo = Arbitrary(genMemo)

  implicit def arbTransaction = Arbitrary(genTransaction)

  implicit def arbSignature = Arbitrary(genSignature)

  implicit def arbThreshold = Arbitrary(genThresholds)

  implicit def arbLedgerResp = Arbitrary(genLedgerResp)

  implicit def arbOfferResp = Arbitrary(genOfferResp)

  implicit def arbOrderBook = Arbitrary(genOrderBook)

  implicit def arbTrade = Arbitrary(genTrade)

  def genKeyPair: Gen[KeyPair] = Gen.oneOf(Seq(KeyPair.random))

  def genSignerKey: Gen[SignerKey] = genKeyPair.map(_.getXDRSignerKey)

  def genPublicKey: Gen[PublicKey] = genKeyPair.map(kp => PublicKey(kp.pk))

  def genAccount: Gen[Account] = for {
    kp <- genKeyPair
    seq <- Gen.posNum[Long]
  } yield {
    Account(kp, seq)
  }

  def genAmount: Gen[Amount] = for {
    units <- Gen.posNum[Long]
    asset <- genAsset
  } yield {
    Amount(units, asset)
  }

  def genNativeAmount: Gen[NativeAmount] = Gen.posNum[Long].map(NativeAmount.apply)

  def genIssuedAmount: Gen[IssuedAmount] = for {
    units <- Gen.posNum[Long]
    asset <- genNonNativeAsset
  } yield {
    IssuedAmount(units, asset)
  }

  def genCode(min: Int, max: Int): Gen[String] = Gen.choose(min, max).map(i => Random.alphanumeric.take(i).mkString)

  def genAsset: Gen[Asset] = Gen.oneOf(genAssetNative, genAsset4, genAsset12)

  def genAssetNative: Gen[Asset] = Gen.oneOf(Seq(NativeAsset))

  def genNonNativeAsset: Gen[NonNativeAsset] = Gen.oneOf(genAsset4, genAsset12)

  def genAsset4: Gen[IssuedAsset4] = for {
    code <- genCode(1, 4)
    issuer <- genKeyPair
  } yield IssuedAsset4.of(code, issuer).get

  def genAsset12: Gen[IssuedAsset12] = for {
    code <- genCode(5, 12)
    issuer <- genKeyPair
  } yield IssuedAsset12.of(code, issuer).get

  def genAssetPath: Gen[Seq[Asset]] = (for {
    qty <- Gen.choose(0, 5)
    path <- Gen.listOfN(qty, genAsset)
  } yield {
    path
  }).suchThat(as => as.distinct.lengthCompare(as.size) == 0)

  def genIssuerFlag: Gen[IssuerFlag] =
    Gen.oneOf(AuthorizationRequiredFlag, AuthorizationRevocableFlag, AuthorizationImmutableFlag)

  def genAccountMergeOperation: Gen[AccountMergeOperation] = for {
    destination <- genPublicKey
  } yield {
    AccountMergeOperation(destination)
  }

  def genAllowTrustOperation = for {
    trustor <- genPublicKey
    assetCode <- Gen.identifier.map(_.take(12))
    authorise <- Gen.oneOf(true, false)
  } yield {
    AllowTrustOperation(trustor, assetCode, authorise)
  }

  def genChangeTrustOperation = for {
    limit <- genIssuedAmount
  } yield {
    ChangeTrustOperation(limit)
  }

  def genCreateAccountOperation = for {
    destination <- genPublicKey
    startingBalance <- genNativeAmount
  } yield {
    CreateAccountOperation(destination, startingBalance)
  }

  def genCreatePassiveOfferOperation = for {
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
  } yield {
    CreatePassiveOfferOperation(selling, buying, price)
  }

  def genInflationOperation = Gen.oneOf(Seq(InflationOperation()))

  def genDeleteDataOperation = for {
    name <- Gen.identifier
  } yield {
    DeleteDataOperation(name)
  }

  def genWriteDataOperation = for {
    name <- Gen.identifier
    value <- Gen.identifier
  } yield {
    WriteDataOperation(name, value)
  }

  def genManageDataOperation = Gen.oneOf(genDeleteDataOperation, genWriteDataOperation)

  def genCreateOfferOperation = for {
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
  } yield {
    CreateOfferOperation(selling, buying, price)
  }

  def genDeleteOfferOperation = for {
    id <- Gen.posNum[Long]
    selling <- genAsset
    buying <- genAsset
    price <- genPrice
  } yield {
    DeleteOfferOperation(id, selling, buying, price)
  }

  def genUpdateOfferOperation = for {
    id <- Gen.posNum[Long]
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
  } yield {
    UpdateOfferOperation(id, selling, buying, price)
  }

  def genManageOfferOperation = Gen.oneOf(genCreateOfferOperation, genDeleteOfferOperation, genUpdateOfferOperation)

  def genPathPaymentOperation = for {
    sendMax <- genAmount
    destAccount <- genPublicKey
    destAmount <- genAmount
    path <- Gen.listOf(genAsset)
  } yield {
    PathPaymentOperation(sendMax, destAccount, destAmount, path)
  }

  def genPaymentOperation = for {
    destAccount <- genPublicKey
    amount <- genAmount
  } yield {
    PaymentOperation(destAccount, amount)
  }

  def genSetOptionsOperation: Gen[SetOptionsOperation] = for {
    inflationDestination <- Gen.option(genPublicKey)
    clearFlags <- Gen.option(Gen.nonEmptyContainerOf[Set, IssuerFlag](genIssuerFlag))
    setFlags <- Gen.option(Gen.nonEmptyContainerOf[Set, IssuerFlag](genIssuerFlag))
    masterKeyWeight <- Gen.option(Gen.choose(0, 255))
    lowThreshold <- Gen.option(Gen.choose(0, 255))
    medThreshold <- Gen.option(Gen.choose(0, 255))
    highThreshold <- Gen.option(Gen.choose(0, 255))
    homeDomain <- Gen.option(Gen.identifier)
    signer <- Gen.option {
      for {
        accn <- genPublicKey
        weight <- Gen.choose(0, 255)
      } yield {
        AccountSigner(accn, weight)
      }
    }
  } yield {
    SetOptionsOperation(inflationDestination, clearFlags, setFlags, masterKeyWeight, lowThreshold, medThreshold,
      highThreshold, homeDomain, signer)
  }

  def genOperation: Gen[Operation] = {
    Gen.oneOf(genAccountMergeOperation, genAllowTrustOperation, genChangeTrustOperation, genCreateAccountOperation,
      genCreatePassiveOfferOperation, genInflationOperation, genManageDataOperation, genManageOfferOperation,
      genPathPaymentOperation, genPaymentOperation, genSetOptionsOperation)
  }

  def genPrice: Gen[Price] = for {
    n <- Gen.posNum[Int]
    d <- Gen.posNum[Int]
  } yield {
    Price(n, d)
  }

  def genInstant: Gen[Instant] = Gen.posNum[Long].map(Instant.ofEpochMilli)

  def genZonedDateTime: Gen[ZonedDateTime] = genInstant.map(ZonedDateTime.ofInstant(_, ZoneId.of("UTC").normalized()))
    .map(_.`with`(ChronoField.NANO_OF_SECOND, 0))

  def genTimeBounds: Gen[TimeBounds] = Gen.listOfN(2, genInstant)
    .suchThat { case List(a, b) => a != b }
    .map(_.sortBy(_.toEpochMilli))
    .map { case List(a, b) => TimeBounds(a, b) }

  def genMemoNone: Gen[Memo] = Gen.oneOf(Seq(NoMemo))

  def genMemoText: Gen[MemoText] = Gen.identifier.map(_.take(28)).map(MemoText.apply)

  def genMemoId: Gen[MemoId] = Gen.posNum[Long].map(MemoId.apply)

  def genMemoHash: Gen[MemoHash] = for {
    bs <- Gen.nonEmptyContainerOf[Array, Byte](Arbitrary.arbByte.arbitrary)
  } yield {
    MemoHash(bs.take(32))
  }

  def genMemoReturnHash: Gen[MemoReturnHash] = for {
    bs <- Gen.nonEmptyContainerOf[Array, Byte](Arbitrary.arbByte.arbitrary)
  } yield {
    MemoReturnHash(bs.take(32))
  }

  def genMemo: Gen[Memo] = Gen.oneOf(genMemoNone, genMemoText, genMemoId, genMemoHash, genMemoReturnHash)

  def genTransaction: Gen[Transaction] = for {
    source <- genAccount
    memo <- genMemo
    operations <- Gen.nonEmptyListOf(genOperation)
    timeBounds <- Gen.option(genTimeBounds)
  } yield {
    Transaction(source, operations, memo, timeBounds)
  }

  def genSignature: Gen[Signature] =
    Gen.nonEmptyContainerOf[Array, Byte](Arbitrary.arbByte.arbitrary).map { bs: Array[Byte] =>
      val s = new Signature
      s.setSignature(bs)
      s
    }

  def genThresholds: Gen[Thresholds] = for {
    low <- Gen.choose(0, 255)
    med <- Gen.choose(0, 255)
    high <- Gen.choose(0, 255)
  } yield {
    Thresholds(low, med, high)
  }

  def genHash = Gen.identifier.map(_.getBytes("UTF-8")).map(Base64.encodeBase64String)

  def genLedgerResp: Gen[LedgerResp] = for {
    id <- Gen.identifier
    hash <- genHash
    previousHash <- Gen.option(genHash)
    sequence <- Gen.posNum[Long]
    transactionCount <- Gen.posNum[Int]
    operationCount <- Gen.posNum[Int]
    closedAt: ZonedDateTime <- genZonedDateTime
    totalCoins <- Gen.posNum[Double].map(round)
    feePool <- Gen.posNum[Double].map(round)
    baseFee <- Gen.posNum[Long]
    baseReserve <- Gen.posNum[Long]
    maxTxSetSize <- Gen.posNum[Int]
  } yield {
    LedgerResp(id, hash, previousHash, sequence, transactionCount, operationCount, closedAt, totalCoins, feePool,
      baseFee, baseReserve, maxTxSetSize)
  }

  def genOfferResp: Gen[OfferResp] = for {
    id <- Gen.posNum[Long]
    seller <- genPublicKey
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
  } yield {
    OfferResp(id, seller, selling, buying, price)
  }

  def genTransacted[O <: Operation](genOp: Gen[O]): Gen[Transacted[O]] = for {
    id <- Gen.posNum[Long]
    hash <- genHash
    source <- genPublicKey
    createdAt <- genZonedDateTime
    op <- genOp
  } yield {
    Transacted(id, hash, source, createdAt, op)
  }

  def genOrder: Gen[Order] = for {
    price <- genPrice
    qty <- Gen.posNum[Long]
  } yield {
    Order(price, qty)
  }

  def genOrderBook: Gen[OrderBook] = for {
    selling <- genAsset
    buying <- if (selling == NativeAsset) genNonNativeAsset else genAsset
    qtyBids <- Gen.choose(0, 20)
    qtyAsks <- Gen.choose(0, 20)
    bids <- Gen.listOfN(qtyBids, genOrder)
    asks <- Gen.listOfN(qtyAsks, genOrder)
  } yield {
    OrderBook(selling, buying, bids, asks)
  }

  def genTrade: Gen[Trade] = for {
    id <- Gen.identifier
    ledgerCloseTime <- genZonedDateTime
    offerId <- Gen.posNum[Long]
    baseAccount <- genPublicKey
    baseAmount <- genAmount
    counterAccount <- genPublicKey
    counterAmount <- genAmount
    baseIsSeller <- Gen.oneOf(true, false)
  } yield {
    Trade(id, ledgerCloseTime, offerId, baseAccount, baseAmount, counterAccount, counterAmount, baseIsSeller)
  }

  def round(d: Double) = f"$d%.7f".toDouble
}

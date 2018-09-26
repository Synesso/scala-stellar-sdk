package stellar.sdk

import java.time.temporal.ChronoField
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.Locale

import org.apache.commons.codec.binary.Base64
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.stellar.sdk.xdr.{Operation => _, _}
import stellar.sdk.ByteArrays.trimmedByteArray
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

  implicit def arbBumpSequenceOperation: Arbitrary[BumpSequenceOperation] = Arbitrary(genBumpSequenceOperation)

  implicit def arbPrice: Arbitrary[Price] = Arbitrary(genPrice)

  implicit def arbOperation: Arbitrary[Operation] = Arbitrary(genOperation)

  implicit def arbInstant: Arbitrary[Instant] = Arbitrary(genInstant)

  implicit def arbTimeBounds: Arbitrary[TimeBounds] = Arbitrary(genTimeBounds)

  implicit def arbMemo = Arbitrary(genMemo)

  implicit def arbTransaction = Arbitrary(genTransaction)

  implicit def arbSignedTransaction = Arbitrary(genSignedTransaction)

  implicit def arbSignature = Arbitrary(genSignature)

  implicit def arbThreshold = Arbitrary(genThresholds)

  implicit def arbAccountResp = Arbitrary(genAccountResp)

  implicit def arbLedgerResp = Arbitrary(genLedgerResp)

  implicit def arbOfferResp = Arbitrary(genOfferResp)

  implicit def arbOrderBook = Arbitrary(genOrderBook)

  implicit def arbTrade = Arbitrary(genTrade)

  implicit def arbTransactionPostResponse = Arbitrary(genTransactionPostResponse)

  implicit def arbTransactionHistoryResponse = Arbitrary(genTransactionHistoryResponse)

  implicit def arbHorizonCursor = Arbitrary(genHorizonCursor)

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
  } yield IssuedAsset4.of(code, issuer)

  def genAsset12: Gen[IssuedAsset12] = for {
    code <- genCode(5, 12)
    issuer <- genKeyPair
  } yield IssuedAsset12.of(code, issuer)

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
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    AccountMergeOperation(destination, sourceAccount)
  }

  def genAllowTrustOperation = for {
    trustor <- genPublicKey
    assetCode <- Gen.identifier.map(_.take(12))
    authorise <- Gen.oneOf(true, false)
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    AllowTrustOperation(trustor, assetCode, authorise, sourceAccount)
  }

  def genChangeTrustOperation = for {
    limit <- genIssuedAmount
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    ChangeTrustOperation(limit, sourceAccount)
  }

  def genCreateAccountOperation = for {
    destination <- genPublicKey
    startingBalance <- genNativeAmount
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    CreateAccountOperation(destination, startingBalance, sourceAccount)
  }

  def genCreatePassiveOfferOperation = for {
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    CreatePassiveOfferOperation(selling, buying, price, sourceAccount)
  }

  def genInflationOperation = for {
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    InflationOperation(sourceAccount)
  }

  def genDeleteDataOperation = for {
    name <- Gen.identifier
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    DeleteDataOperation(name, sourceAccount)
  }

  def genWriteDataOperation = for {
    name <- Gen.identifier
    value <- Gen.identifier
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    WriteDataOperation(name, value, sourceAccount)
  }

  def genManageDataOperation = Gen.oneOf(genDeleteDataOperation, genWriteDataOperation)

  def genCreateOfferOperation = for {
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    CreateOfferOperation(selling, buying, price, sourceAccount)
  }

  def genDeleteOfferOperation = for {
    id <- Gen.posNum[Long]
    selling <- genAsset
    buying <- genAsset
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    DeleteOfferOperation(id, selling, buying, price, sourceAccount)
  }

  def genUpdateOfferOperation = for {
    id <- Gen.posNum[Long]
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    UpdateOfferOperation(id, selling, buying, price, sourceAccount)
  }

  def genManageOfferOperation = Gen.oneOf(genCreateOfferOperation, genDeleteOfferOperation, genUpdateOfferOperation)

  def genPathPaymentOperation = for {
    sendMax <- genAmount
    destAccount <- genPublicKey
    destAmount <- genAmount
    path <- Gen.listOf(genAsset)
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    PathPaymentOperation(sendMax, destAccount, destAmount, path, sourceAccount)
  }

  def genPaymentOperation = for {
    destAccount <- genPublicKey
    amount <- genAmount
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    PaymentOperation(destAccount, amount, sourceAccount)
  }

  def genPayOperation: Gen[PayOperation] = Gen.oneOf(genPaymentOperation, genCreateAccountOperation)

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
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    SetOptionsOperation(inflationDestination, clearFlags, setFlags, masterKeyWeight, lowThreshold, medThreshold,
      highThreshold, homeDomain, signer, sourceAccount)
  }

  def genBumpSequenceOperation: Gen[BumpSequenceOperation] = for {
    sequence <- Gen.posNum[Long]
    sourceAccount <- Gen.option(genPublicKey)
  } yield BumpSequenceOperation(sequence, sourceAccount)

  def genOperation: Gen[Operation] = {
    Gen.oneOf(genAccountMergeOperation, genAllowTrustOperation, genChangeTrustOperation, genCreateAccountOperation,
      genCreatePassiveOfferOperation, genInflationOperation, genManageDataOperation, genManageOfferOperation,
      genPathPaymentOperation, genPaymentOperation, genSetOptionsOperation, genBumpSequenceOperation)
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
    MemoHash(trimmedByteArray(bs.take(32)))
  }

  def genMemoReturnHash: Gen[MemoReturnHash] = for {
    bs <- Gen.nonEmptyContainerOf[Array, Byte](Arbitrary.arbByte.arbitrary)
  } yield {
    MemoReturnHash(trimmedByteArray(bs.take(32)))
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

  def genSignedTransaction: Gen[SignedTransaction] = for {
    txn <- genTransaction
    signer <- genKeyPair
  } yield txn.sign(signer)

  def genThresholds: Gen[Thresholds] = for {
    low <- Gen.choose(0, 255)
    med <- Gen.choose(0, 255)
    high <- Gen.choose(0, 255)
  } yield {
    Thresholds(low, med, high)
  }

  def genHash = Gen.identifier.map(_.getBytes("UTF-8")).map(Base64.encodeBase64String)

  def genAccountResp: Gen[AccountResp] = for {
    id <- genPublicKey
    lastSequence <- Gen.posNum[Long]
    subEntryCount <- Gen.posNum[Int]
    thresholds <- genThresholds
    authRequired <- Gen.oneOf(true, false)
    authRevocable <- Gen.oneOf(true, false)
    balances <- Gen.nonEmptyListOf(genBalance)
    signers <- Gen.nonEmptyListOf(genSigner)
  } yield AccountResp(id, lastSequence, subEntryCount, thresholds, authRequired, authRevocable, balances, signers)

  def genSigner: Gen[Signer] = Gen.oneOf(genAccountSigner, genHashSigner, genPreAuthTxnSigner)

  def genBalance: Gen[Balance] = for {
    amount <- genAmount
    limit <- Gen.choose(amount.units, Long.MaxValue).map(Some(_)).map(_.filterNot(_ => amount.asset == NativeAsset))
    buyingLiabilities <- Gen.choose(0, amount.units)
    sellingLiabilities <- Gen.choose(0, amount.units)
  } yield Balance(amount, limit, buyingLiabilities = buyingLiabilities, sellingLiabilities)

  def genAccountSigner: Gen[AccountSigner] = for {
    key <- genPublicKey
    weight <- Gen.posNum[Int]
  } yield AccountSigner(key, weight)

  def genHashSigner: Gen[HashSigner] = for {
    hash <- genHash
    weight <- Gen.posNum[Int]
  } yield HashSigner(hash, weight)

  def genPreAuthTxnSigner: Gen[PreAuthTxnSigner] = for {
    hash <- genHash
    weight <- Gen.posNum[Int]
  } yield PreAuthTxnSigner(hash, weight)

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
    lastModifiedLedger <- Gen.posNum[Long]
    lastModifiedTime <- genZonedDateTime
  } yield {
    OfferResp(id, seller, selling, buying, price, lastModifiedLedger, lastModifiedTime)
  }

  def genTransacted[O <: Operation](genOp: Gen[O]): Gen[Transacted[O]] = for {
    id <- Gen.posNum[Long]
    hash <- genHash
    createdAt <- genZonedDateTime
    op <- genOp
  } yield {
    Transacted(id, hash, createdAt, op)
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

  def genTransactionPostResponse: Gen[TransactionPostResp] = for {
    hash <- genHash
    ledger <- Gen.posNum[Long]
    envelopeXDR <- genHash
    resultXDR <- genHash
    resultMetaXDR <- genHash
  } yield TransactionPostResp(hash, ledger, envelopeXDR, resultXDR, resultMetaXDR)

  def genTransactionHistoryResponse: Gen[TransactionHistoryResp] = for {
    hash <- genHash
    ledger <- Gen.posNum[Long]
    createdAt <- genZonedDateTime
    account <- genPublicKey
    sequence <- Gen.posNum[Long]
    feePaid <- Gen.posNum[Int]
    operationCount <- Gen.posNum[Int]
    memo <- genMemo.map {
      case MemoReturnHash(bs) => MemoHash(bs)
      case m => m
    }
    signatures <- Gen.nonEmptyListOf(genHash)
    envelopeXDR <- genHash
    resultXDR <- genHash
    resultMetaXDR <- genHash
    feeMetaXDR <- genHash
  } yield TransactionHistoryResp(hash, ledger, createdAt, account, sequence, feePaid, operationCount,
    memo, signatures, envelopeXDR, resultXDR, resultMetaXDR, feeMetaXDR)

  def genHorizonCursor: Gen[HorizonCursor] = Gen.option(Gen.posNum[Long]).map(_.map(Record).getOrElse(Now))

  def round(d: Double) = "%.7f".formatLocal(Locale.ROOT, d).toDouble
}

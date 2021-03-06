package stellar.sdk

import java.nio.charset.StandardCharsets.UTF_8
import java.time.temporal.ChronoField
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.Locale

import okhttp3.HttpUrl
import okio.ByteString
import org.apache.commons.codec.binary.Base64
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.stellar.xdr.ManageOfferEffect.MANAGE_OFFER_DELETED
import org.stellar.xdr.{ManageOfferEffect, TransactionResultCode}
import stellar.sdk.key._
import stellar.sdk.model._
import stellar.sdk.model.ledger._
import stellar.sdk.model.op._
import stellar.sdk.model.response._
import stellar.sdk.model.result._
import stellar.sdk.util.{ByteArrays, DoNothingNetwork}

import scala.annotation.tailrec
import scala.util.Random

trait ArbitraryInput extends ScalaCheck {

  implicit val network: Network = PublicNetwork

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

  implicit def arbCreatePassiveSellOfferOperation: Arbitrary[CreatePassiveSellOfferOperation] = Arbitrary(genCreatePassiveSellOfferOperation)

  implicit def arbWriteDataOperation: Arbitrary[WriteDataOperation] = Arbitrary(genWriteDataOperation)

  implicit def arbDeleteDataOperation: Arbitrary[DeleteDataOperation] = Arbitrary(genDeleteDataOperation)

  implicit def arbCreateBuyOfferOperation: Arbitrary[CreateBuyOfferOperation] = Arbitrary(genCreateBuyOfferOperation)
  implicit def arbDeleteBuyOfferOperation: Arbitrary[DeleteBuyOfferOperation] = Arbitrary(genDeleteBuyOfferOperation)
  implicit def arbUpdateBuyOfferOperation: Arbitrary[UpdateBuyOfferOperation] = Arbitrary(genUpdateBuyOfferOperation)

  implicit def arbCreateSellOfferOperation: Arbitrary[CreateSellOfferOperation] = Arbitrary(genCreateSellOfferOperation)
  implicit def arbDeleteSellOfferOperation: Arbitrary[DeleteSellOfferOperation] = Arbitrary(genDeleteSellOfferOperation)
  implicit def arbUpdateSellOfferOperation: Arbitrary[UpdateSellOfferOperation] = Arbitrary(genUpdateSellOfferOperation)

  implicit def arbInflationOperation: Arbitrary[InflationOperation] = Arbitrary(genInflationOperation)

  implicit def arbPathPaymentStrictReceiveOperation: Arbitrary[PathPaymentStrictReceiveOperation] = Arbitrary(genPathPaymentStrictReceiveOperation)

  implicit def arbPathPaymentStrictSendOperation: Arbitrary[PathPaymentStrictSendOperation] = Arbitrary(genPathPaymentStrictSendOperation)

  implicit def arbPaymentOperation: Arbitrary[PaymentOperation] = Arbitrary(genPaymentOperation)

  implicit def arbSetOptionsOperation: Arbitrary[SetOptionsOperation] = Arbitrary(genSetOptionsOperation)

  implicit def arbBumpSequenceOperation: Arbitrary[BumpSequenceOperation] = Arbitrary(genBumpSequenceOperation)

  implicit def arbPrice: Arbitrary[Price] = Arbitrary(genPrice)

  implicit def arbOperation: Arbitrary[Operation] = Arbitrary(genOperation)

  implicit def arbInstant: Arbitrary[Instant] = Arbitrary(genInstant)

  implicit def arbTimeBounds: Arbitrary[TimeBounds] = Arbitrary(genTimeBounds)

  implicit def arbMemo: Arbitrary[Memo] = Arbitrary(genMemo)

  implicit def arbTransaction: Arbitrary[Transaction] = Arbitrary(genTransaction)

  implicit def arbSignedTransaction: Arbitrary[SignedTransaction] = Arbitrary(genSignedTransaction)

  implicit def arbThreshold: Arbitrary[Thresholds] = Arbitrary(genThresholds)

  implicit def arbAccountResp: Arbitrary[AccountResponse] = Arbitrary(genAccountResp)

  implicit def arbLedgerResp: Arbitrary[LedgerResponse] = Arbitrary(genLedgerResp)

  implicit def arbOfferResp: Arbitrary[OfferResponse] = Arbitrary(genOfferResp)

  implicit def arbOrderBook: Arbitrary[OrderBook] = Arbitrary(genOrderBook)

  implicit def arbTrade: Arbitrary[Trade] = Arbitrary(genTrade)

  implicit def arbTransactionPostResponse: Arbitrary[TransactionApproved] = Arbitrary(genTransactionPostSuccess)

  implicit def arbTransactionHistory: Arbitrary[TransactionHistory] = Arbitrary(genTransactionHistory)

  implicit def arbHorizonCursor: Arbitrary[HorizonCursor] = Arbitrary(genHorizonCursor)

  implicit def arbOperationResult: Arbitrary[OperationResult] = Arbitrary(genOperationResult)

  implicit def arbAccountMergeResult: Arbitrary[AccountMergeResult] = Arbitrary(genAccountMergeResult)

  implicit def arbAllowTrustResult: Arbitrary[AllowTrustResult] = Arbitrary(genAllowTrustResult)

  implicit def arbBumpSequenceResult: Arbitrary[BumpSequenceResult] = Arbitrary(genBumpSequenceResult)

  implicit def arbChangeTrustResult: Arbitrary[ChangeTrustResult] = Arbitrary(genChangeTrustResult)

  implicit def arbCreateAccountResult: Arbitrary[CreateAccountResult] = Arbitrary(genCreateAccountResult)

  implicit def arbCreateClaimableBalanceResult: Arbitrary[CreateClaimableBalanceResult] = Arbitrary(genCreateClaimableBalanceResult)

  implicit def arbClaimClaimableBalanceResult: Arbitrary[ClaimClaimableBalanceResult] = Arbitrary(genClaimClaimableBalanceResult)

  implicit def arbCreatePassiveSellOfferResult: Arbitrary[CreatePassiveSellOfferResult] = Arbitrary(genCreatePassiveSellOfferResult)

  implicit def arbInflationResult: Arbitrary[InflationResult] = Arbitrary(genInflationResult)

  implicit def arbManageDataResult: Arbitrary[ManageDataResult] = Arbitrary(genManageDataResult)

  implicit def arbManageBuyOfferResult: Arbitrary[ManageBuyOfferResult] = Arbitrary(genManageBuyOfferResult)

  implicit def arbManageSellOfferResult: Arbitrary[ManageSellOfferResult] = Arbitrary(genManageSellOfferResult)

  implicit def arbPathPaymentReceiveResult: Arbitrary[PathPaymentReceiveResult] = Arbitrary(genPathPaymentReceiveResult)

  implicit def arbPathPaymentSendResult: Arbitrary[PathPaymentSendResult] = Arbitrary(genPathPaymentSendResult)

  implicit def arbPaymentResult: Arbitrary[PaymentResult] = Arbitrary(genPaymentResult)

  implicit def arbSetOptionsResult: Arbitrary[SetOptionsResult] = Arbitrary(genSetOptionsResult)

  implicit def arbTransactionResult: Arbitrary[TransactionResult] = Arbitrary(genTransactionResult)

  implicit def arbTransactionSuccess: Arbitrary[TransactionSuccess] = Arbitrary(genTransactionSuccess)

  implicit def arbTransactionNotSuccessful: Arbitrary[TransactionNotSuccessful] = Arbitrary(genTransactionNotSuccessful)

  implicit def arbSigner: Arbitrary[Signer] = Arbitrary(genSigner)

  implicit def arbStrKey: Arbitrary[StrKey] = Arbitrary(genStrKey)

  implicit def arbSignerStrKey: Arbitrary[SignerStrKey] = Arbitrary(genSignerStrKey)

  implicit def arbFeeStatsResponse: Arbitrary[FeeStatsResponse] = Arbitrary(genFeeStatsResponse)

  implicit def arbNetworkInfo: Arbitrary[NetworkInfo] = Arbitrary(genNetworkInfo)

  implicit def arbFederationResponse: Arbitrary[FederationResponse] = Arbitrary(genFederationResponse)

  implicit def arbPaymentPath: Arbitrary[PaymentPath] = Arbitrary(genPaymentPath)

  implicit def arbTradeAggregation: Arbitrary[TradeAggregation] = Arbitrary(genTradeAggregation)

  implicit def arbWordList: Arbitrary[WordList] = Arbitrary(
    Gen.oneOf(EnglishWords, FrenchWords, JapaneseWords, SpanishWords))

  implicit def arbPaymentSigningRequest: Arbitrary[PaymentSigningRequest] = Arbitrary(genPaymentSigningRequest)

  implicit def arbTransactionSigningRequest: Arbitrary[TransactionSigningRequest] =
    Arbitrary(genTransactionSigningRequest)

  implicit def arbZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary(genZonedDateTime)

  def genListOfNM[T](low: Int, high: Int, gen: Gen[T]): Gen[List[T]] = for {
    size <- Gen.choose(low, high)
    xs <- Gen.listOfN(size, gen)
  } yield xs

  def round(d: Double): Double = "%.7f".formatLocal(Locale.ROOT, d).toDouble

  def genKeyPair: Gen[KeyPair] = Gen.delay(Gen.const(KeyPair.random))

  def genPublicKey: Gen[PublicKey] = genKeyPair.map(kp => PublicKey(kp.pk))

  def genAccountId: Gen[AccountId] = for {
    key <- genPublicKey.map(_.publicKey)
    subAccountId <- Gen.option(Gen.posNum[Long])
  } yield AccountId(key, subAccountId)

  def genAccount: Gen[Account] = for {
    kp <- genAccountId
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
    destination <- genPublicKey.map(_.publicKey).map(AccountId(_))
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    AccountMergeOperation(destination, sourceAccount)
  }

  def genAllowTrustOperation: Gen[AllowTrustOperation] = for {
    trustor <- genPublicKey
    assetCode <- Gen.identifier.map(_.take(12))
    authorise <- Gen.choose(0, 3).map(TrustLineFlags.from)
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    AllowTrustOperation(trustor, assetCode, authorise, sourceAccount)
  }

  def genChangeTrustOperation: Gen[ChangeTrustOperation] = for {
    limit <- genIssuedAmount
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    ChangeTrustOperation(limit, sourceAccount)
  }

  def genCreateAccountOperation: Gen[CreateAccountOperation] = for {
    destination <- genPublicKey.map(_.toAccountId)
    startingBalance <- genNativeAmount
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    CreateAccountOperation(destination, startingBalance, sourceAccount)
  }

  def genCreatePassiveSellOfferOperation: Gen[CreatePassiveSellOfferOperation] = for {
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    CreatePassiveSellOfferOperation(selling, buying, price, sourceAccount)
  }

  def genInflationOperation: Gen[InflationOperation] = for {
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    InflationOperation(sourceAccount)
  }

  def genDeleteDataOperation: Gen[DeleteDataOperation] = for {
    name <- Gen.identifier.map(_.take(64))
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    DeleteDataOperation(name, sourceAccount)
  }

  def genWriteDataOperation: Gen[WriteDataOperation] = for {
    nameLen <- Gen.choose(1, 64)
    valueLen <- Gen.choose(1, 64)
    name <- Gen.listOfN(nameLen, Gen.alphaNumChar).map(_.mkString)
    value <- Gen.listOfN(valueLen, Arbitrary.arbByte.arbitrary).map(_.toIndexedSeq)
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    WriteDataOperation(name, value, sourceAccount)
  }

  def genManageDataOperation: Gen[ManageDataOperation] = Gen.oneOf(genDeleteDataOperation, genWriteDataOperation)

  def genCreateSellOfferOperation: Gen[CreateSellOfferOperation] = for {
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    CreateSellOfferOperation(selling, buying, price, sourceAccount)
  }

  def genDeleteSellOfferOperation: Gen[DeleteSellOfferOperation] = for {
    id <- Gen.posNum[Long]
    selling <- genAsset
    buying <- genAsset
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    DeleteSellOfferOperation(id, selling, buying, price, sourceAccount)
  }

  def genUpdateSellOfferOperation: Gen[UpdateSellOfferOperation] = for {
    id <- Gen.posNum[Long]
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    UpdateSellOfferOperation(id, selling, buying, price, sourceAccount)
  }

  def genManageSellOfferOperation: Gen[ManageSellOfferOperation] =
    Gen.oneOf(genCreateSellOfferOperation, genDeleteSellOfferOperation, genUpdateSellOfferOperation)

  def genCreateBuyOfferOperation: Gen[CreateBuyOfferOperation] = for {
    selling <- genAsset
    buying <- genAmount
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    CreateBuyOfferOperation(selling, buying, price, sourceAccount)
  }

  def genDeleteBuyOfferOperation: Gen[DeleteBuyOfferOperation] = for {
    id <- Gen.posNum[Long]
    selling <- genAsset
    buying <- genAsset
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    DeleteBuyOfferOperation(id, selling, buying, price, sourceAccount)
  }

  def genUpdateBuyOfferOperation: Gen[UpdateBuyOfferOperation] = for {
    id <- Gen.posNum[Long]
    selling <- genAsset
    buying <- genAmount
    price <- genPrice
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    UpdateBuyOfferOperation(id, selling, buying, price, sourceAccount)
  }

  def genManageBuyOfferOperation: Gen[ManageBuyOfferOperation] =
    Gen.oneOf(genCreateBuyOfferOperation, genDeleteBuyOfferOperation, genUpdateBuyOfferOperation)

  def genPathPaymentStrictReceiveOperation: Gen[PathPaymentStrictReceiveOperation] = for {
    sendMax <- genAmount
    destAccount <- genAccountId
    destAmount <- genAmount
    path <- Gen.listOf(genAsset)
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    PathPaymentStrictReceiveOperation(sendMax, destAccount, destAmount, path, sourceAccount)
  }

  def genPathPaymentStrictSendOperation: Gen[PathPaymentStrictSendOperation] = for {
    sendAmount <- genAmount
    destAccount <- genAccountId
    destinationMin <- genAmount
    path <- Gen.listOf(genAsset)
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    PathPaymentStrictSendOperation(sendAmount, destAccount, destinationMin, path, sourceAccount)
  }

  def genPaymentOperation: Gen[PaymentOperation] = for {
    destAccount <- genAccountId
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
    homeDomain <- Gen.option(Gen.identifier.map(_.take(32)))
    signerWeight <- Gen.choose(0, 255)
    signer <- Gen.option(genSignerStrKey.map(Signer(_, signerWeight)))
    sourceAccount <- Gen.option(genPublicKey)
  } yield {
    SetOptionsOperation(inflationDestination, clearFlags, setFlags, masterKeyWeight, lowThreshold, medThreshold,
      highThreshold, homeDomain, signer, sourceAccount)
  }

  def genAccountIdStrKey: Gen[AccountId] = for {
    pk <- genPublicKey
  } yield AccountId(pk.publicKey.toList)

  def genSeedStrKey: Gen[Seed] = genKeyPair.map(kp => Seed(kp.sk.getAbyte.toIndexedSeq))
  def genPreAuthTxStrKey: Gen[PreAuthTx] = Gen.containerOfN[Array, Byte](32, Arbitrary.arbByte.arbitrary)
    .map(bs => PreAuthTx(bs.toIndexedSeq))
  def genHashStrKey: Gen[SHA256Hash] = Gen.identifier.map(_.getBytes("UTF-8")).map(ByteArrays.sha256)
    .map(bs => SHA256Hash(bs.toIndexedSeq))

  def genStrKey: Gen[StrKey] = Gen.oneOf(genAccountIdStrKey, genSeedStrKey, genPreAuthTxStrKey, genHashStrKey)

  def genSignerStrKey: Gen[SignerStrKey] = Gen.oneOf(genAccountIdStrKey, genPreAuthTxStrKey, genHashStrKey)

  def genBumpSequenceOperation: Gen[BumpSequenceOperation] = for {
    sequence <- Gen.posNum[Long]
    sourceAccount <- Gen.option(genPublicKey)
  } yield BumpSequenceOperation(sequence, sourceAccount)

  def genCreateClaimableBalanceOperation: Gen[CreateClaimableBalanceOperation] = for {
    amount <- genAmount
    claimants <- Gen.nonEmptyListOf(ClaimantGenerators.genClaimant).map(_.take(10))
    sourceAccount <- Gen.option(genPublicKey)
  } yield CreateClaimableBalanceOperation(amount, claimants, sourceAccount)

  def genClaimClaimableBalanceOperation: Gen[ClaimClaimableBalanceOperation] = for {
    id <- ClaimableBalanceIds.genClaimableBalanceId
    sourceAccount <- Gen.option(genPublicKey)
  } yield ClaimClaimableBalanceOperation(id, sourceAccount)

  def genBeginSponsoringFutureReservesOperation: Gen[BeginSponsoringFutureReservesOperation] = for {
    sponsored <- genPublicKey.map(_.publicKey).map(AccountId(_))
    sourceAccount <- Gen.option(genPublicKey)
  } yield BeginSponsoringFutureReservesOperation(sponsored, sourceAccount)

  def genEndSponsoringFutureReservesOperation: Gen[EndSponsoringFutureReservesOperation] =
    Gen.option(genPublicKey).map(EndSponsoringFutureReservesOperation.apply)

  def genRevokeSponsorshipOperation: Gen[RevokeSponsorshipOperation] = Gen.oneOf(
    genRevokeAccountSponsorshipOperation,
    genRevokeClaimableBalanceSponsorshipOperation,
    genRevokeDataSponsorshipOperation,
    genRevokeOfferSponsorshipOperation,
    genRevokeSignerSponsorshipOperation,
    genRevokeTrustLineSponsorshipOperation
  )

  def genRevokeAccountSponsorshipOperation: Gen[RevokeAccountSponsorshipOperation] = for {
    key <- genPublicKey.map(AccountKey.apply)
    sourceAccount <- Gen.option(genPublicKey)
  } yield RevokeAccountSponsorshipOperation(key, sourceAccount)
  
  def genRevokeClaimableBalanceSponsorshipOperation: Gen[RevokeClaimableBalanceSponsorshipOperation] = for {
    key <- ClaimableBalanceIds.genClaimableBalanceId.map(ClaimableBalanceKey.apply)
    sourceAccount <- Gen.option(genPublicKey)
  } yield RevokeClaimableBalanceSponsorshipOperation(key, sourceAccount)
  
  def genRevokeDataSponsorshipOperation: Gen[RevokeDataSponsorshipOperation] = for {
    key <- for {
      account <- genPublicKey
      name <- Gen.identifier.map(_.take(64))
    } yield DataKey(account, name)
    sourceAccount <- Gen.option(genPublicKey)
  } yield RevokeDataSponsorshipOperation(key, sourceAccount)
  
  def genRevokeOfferSponsorshipOperation: Gen[RevokeOfferSponsorshipOperation] = for {
    key <- for {
      pk <- genPublicKey
      offerId <- Gen.posNum[Long]
    } yield OfferKey(pk, offerId)
    sourceAccount <- Gen.option(genPublicKey)
  } yield RevokeOfferSponsorshipOperation(key, sourceAccount)
  
  def genRevokeSignerSponsorshipOperation: Gen[RevokeSignerSponsorshipOperation] = for {
    accountId <- genPublicKey.map(_.toAccountId)
    signerKey <- genSignerStrKey
    sourceAccount <- Gen.option(genPublicKey)
  } yield RevokeSignerSponsorshipOperation(accountId, signerKey, sourceAccount)
  
  def genRevokeTrustLineSponsorshipOperation: Gen[RevokeTrustLineSponsorshipOperation] = for {
    key <- for {
      pk <- genPublicKey
      asset <- genNonNativeAsset
    } yield TrustLineKey(pk, asset)
    sourceAccount <- Gen.option(genPublicKey)
  } yield RevokeTrustLineSponsorshipOperation(key, sourceAccount)
  
  def genOperation: Gen[Operation] = {
    Gen.oneOf(genAccountMergeOperation, genAllowTrustOperation, genChangeTrustOperation, genCreateAccountOperation,
      genCreatePassiveSellOfferOperation, genInflationOperation, genManageDataOperation, genManageSellOfferOperation,
      genManageBuyOfferOperation, genPathPaymentStrictReceiveOperation, genPaymentOperation, genSetOptionsOperation,
      genBumpSequenceOperation, genPathPaymentStrictSendOperation, genCreateClaimableBalanceOperation,
      genClaimClaimableBalanceOperation, genBeginSponsoringFutureReservesOperation,
      genEndSponsoringFutureReservesOperation, genRevokeSponsorshipOperation)
  }

  def genPrice: Gen[Price] = for {
    n <- Gen.posNum[Int]
    d <- Gen.posNum[Int]
  } yield {
    Price(n, d)
  }

  def genInstant: Gen[Instant] = Arbitrary.arbInt.arbitrary.map(_.toLong)
    .map(Instant.now().`with`(ChronoField.MILLI_OF_SECOND, 0).plusSeconds)

  def genZonedDateTime: Gen[ZonedDateTime] = genInstant.map(ZonedDateTime.ofInstant(_, ZoneId.of("UTC").normalized()))
    .map(_.`with`(ChronoField.NANO_OF_SECOND, 0))

  def genTimeBounds: Gen[TimeBounds] = Gen.listOfN(2, genInstant)
    .suchThat { case List(a, b) => a != b }
    .map(_.sortBy(_.toEpochMilli))
    .map { case List(a, b) => TimeBounds(a, b) }

  def genMemoNone: Gen[Memo] = Gen.oneOf(Seq(NoMemo))

  def genMemoText: Gen[MemoText] = for {
      n <- Gen.chooseNum(1, 28)
      bs <- Gen.identifier.map(_.take(n).getBytes(UTF_8)).map(new ByteString(_))
    } yield MemoText(bs)

  def genMemoId: Gen[MemoId] = Arbitrary.arbLong.arbitrary.map(MemoId.apply)

  def genMemoHash: Gen[MemoHash] = Gen.containerOfN[Array, Byte](32, Arbitrary.arbByte.arbitrary)
    .map(new ByteString(_))
    .map(MemoHash(_))

  def genMemoReturnHash: Gen[MemoReturnHash] = Gen.containerOfN[Array, Byte](32, Arbitrary.arbByte.arbitrary)
    .map(new ByteString(_))
    .map(MemoReturnHash(_))

  def genMemo: Gen[Memo] = Gen.oneOf(genMemoNone, genMemoText, genMemoId, genMemoHash, genMemoReturnHash)

  def genTransaction: Gen[Transaction] = genTransaction(PublicNetwork)

  def genTransaction(network: Network): Gen[Transaction] = for {
    source <- genAccount
    memo <- genMemo
    operations <- Gen.nonEmptyListOf(genOperation)
    timeBounds <- genTimeBounds
    maxFee <- genNativeAmount.map(a => NativeAmount(math.max(a.units, operations.size * 100)))
  } yield {
    Transaction(source, operations, memo, timeBounds, maxFee)(network)
  }

  def genSignedTransaction: Gen[SignedTransaction] = genSignedTransaction(PublicNetwork)

  def genSignedTransaction(network: Network): Gen[SignedTransaction] = for {
    txn <- genTransaction(network)
    signer <- genKeyPair
    feeBump <- Gen.option(genFeeBump)
  } yield txn.sign(signer).copy(feeBump = feeBump)

  def genNetwork: Gen[Network] = Gen.identifier.map(new DoNothingNetwork(_))

  def genThresholds: Gen[Thresholds] = for {
    low <- Gen.choose(0, 255)
    med <- Gen.choose(0, 255)
    high <- Gen.choose(0, 255)
  } yield {
    Thresholds(low, med, high)
  }

  def genFeeBump: Gen[FeeBump] = for {
    source <- genAccountId
    fee <- genNativeAmount
    payload <- Gen.containerOf[Array, Byte](Gen.posNum[Byte])
    signatures <- Gen.nonEmptyListOf(genKeyPair.map(_.sign(payload))).map(_.take(3))
  } yield FeeBump(source, fee, signatures)

  def genHash: Gen[String] = Gen.containerOfN[Array, Char](32, Gen.alphaNumChar)
    .map(_.map(_.toByte)).map(Base64.encodeBase64String)

  def genAccountResp: Gen[AccountResponse] = for {
    id <- genPublicKey
    lastSequence <- Gen.posNum[Long]
    subEntryCount <- Gen.posNum[Int]
    thresholds <- genThresholds
    authRequired <- Gen.oneOf(true, false)
    authRevocable <- Gen.oneOf(true, false)
    balances <- Gen.nonEmptyListOf(genBalance)
    signers <- Gen.nonEmptyListOf(genSigner)
    sponsor <- Gen.option(genPublicKey)
    sponsoring <- Gen.chooseNum(0, 20)
    sponsored <- Gen.chooseNum(0, 20)
    data <- genDataMap
  } yield AccountResponse(id, lastSequence, subEntryCount, thresholds, authRequired, authRevocable, balances, signers,
    sponsor, sponsored, sponsoring, data)

  def genDataMap: Gen[Map[String, Array[Byte]]] = for {
    qty <- Gen.choose(0, 30)
    keys <- Gen.listOfN(qty, Gen.identifier)
    values <- Gen.listOfN(qty, Arbitrary.arbString.arbitrary.map(_.take(60).getBytes(UTF_8)))
  } yield keys.zip(values).toMap

  def genSigner: Gen[Signer] = for {
    weight <- Gen.choose(0, 255)
    strKey <- genSignerStrKey
  } yield Signer(strKey, weight)

  def genBalance: Gen[Balance] = for {
    amount <- genAmount
    limit <- Gen.choose(amount.units, Long.MaxValue).map(Some(_)).map(_.filterNot(_ => amount.asset == NativeAsset))
    buyingLiabilities <- Gen.choose(0, amount.units)
    sellingLiabilities <- Gen.choose(0, amount.units)
    authorized <- Gen.oneOf(true, false)
    authorizedToMaintainLiabilities <- Gen.oneOf(true, false)
    sponsor <- Gen.option(genPublicKey)
  } yield Balance(amount, limit, buyingLiabilities = buyingLiabilities, sellingLiabilities, authorized,
    authorizedToMaintainLiabilities, sponsor)

  def genLedgerResp: Gen[LedgerResponse] = for {
    id <- Gen.identifier
    hash <- genHash
    previousHash <- Gen.option(genHash)
    sequence <- Gen.posNum[Long]
    successTransactionCount <- Gen.choose(0, 100)
    failedTransactionCount <- Gen.choose(0, 100 - successTransactionCount)
    operationCount <- Gen.posNum[Int]
    closedAt: ZonedDateTime <- genZonedDateTime
    totalCoins <- genNativeAmount
    feePool <- genNativeAmount
    baseFee <- genNativeAmount
    baseReserve <- genNativeAmount
    maxTxSetSize <- Gen.posNum[Int]
  } yield {
    LedgerResponse(id, hash, previousHash, sequence, successTransactionCount, failedTransactionCount, operationCount,
      closedAt, totalCoins, feePool, baseFee, baseReserve, maxTxSetSize)
  }

  def genOfferResp: Gen[OfferResponse] = for {
    id <- Gen.posNum[Long]
    seller <- genPublicKey
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
    lastModifiedLedger <- Gen.posNum[Long]
    lastModifiedTime <- genZonedDateTime
    sponsor <- Gen.option(genPublicKey)
  } yield {
    OfferResponse(id, seller, selling, buying, price, lastModifiedLedger, lastModifiedTime, sponsor)
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
    baseOfferId <- Gen.posNum[Long]
    counterOfferId <- Gen.posNum[Long]
    baseAccount <- genPublicKey
    baseAmount <- genAmount
    counterAccount <- genPublicKey
    counterAmount <- genAmount
    baseIsSeller <- Gen.oneOf(true, false)
  } yield {
    Trade(id, ledgerCloseTime, offerId, baseOfferId, counterOfferId, baseAccount, baseAmount, counterAccount, counterAmount, baseIsSeller)
  }

  def genTradeAggregation: Gen[TradeAggregation] = for {
    instant <- genInstant
    tradeCount <- Gen.posNum[Int]
    baseVolume <- Gen.posNum[Double]
    counterVolume <- Gen.posNum[Double]
    average <- Gen.posNum[Double]
    prices <- Gen.listOfN(4, genPrice)
    Seq(low, open, close, high) = prices
  } yield TradeAggregation(instant, tradeCount, baseVolume, counterVolume, average, open, high, low, close)

  def genTransactionPostSuccess: Gen[TransactionApproved] = for {
    hash <- genHash
    ledger <- Gen.posNum[Long]
    envelopeXDR <- genHash
    resultXDR <- genHash
    resultMetaXDR <- genHash
  } yield TransactionApproved(hash, ledger, envelopeXDR, resultXDR, resultMetaXDR)

  def genTransactionHistory: Gen[TransactionHistory] = for {
    hash <- genHash
    ledger <- Gen.posNum[Long]
    createdAt <- genZonedDateTime
    account <- genPublicKey
    sequence <- Gen.posNum[Long]
    maxFee <- Gen.posNum[Int].map(NativeAmount(_))
    feeCharged <- Gen.posNum[Int].map(NativeAmount(_))
    operationCount <- Gen.posNum[Int]
    memo <- genMemo
    signatures <- Gen.nonEmptyListOf(genHash)
    envelopeXDR <- genHash
    resultXDR <- genHash
    resultMetaXDR <- genHash
    feeMetaXDR <- genHash
    validAfter <- Gen.option(genZonedDateTime)
    validBefore <- Gen.option(genZonedDateTime)
    feeBumpHistory <- Gen.option(genFeeBumpHistory)
  } yield TransactionHistory(hash, ledger, createdAt, account, sequence, maxFee, feeCharged, operationCount,
    memo, signatures, envelopeXDR, resultXDR, resultMetaXDR, feeMetaXDR, validAfter, validBefore, feeBumpHistory)

  def genFeeBumpHistory: Gen[FeeBumpHistory] = for {
    maxFee <- genNativeAmount
    hash <- genHash
    signatures <- Gen.nonEmptyListOf(genHash)
  } yield FeeBumpHistory(maxFee, hash, signatures)

  def genHorizonCursor: Gen[HorizonCursor] = Gen.option(Gen.posNum[Long]).map(_.map(Record).getOrElse(Now))

  // === Operation Results ===
  def genOperationResult: Gen[OperationResult] = Gen.oneOf(
    genAccountMergeResult, genAllowTrustResult, genBumpSequenceResult, genChangeTrustResult,
    genCreatePassiveSellOfferResult, genInflationResult, genManageDataResult, genManageSellOfferResult,
    genPathPaymentReceiveResult, genPaymentResult, genSetOptionsResult, genOperationNotAttemptedResult,
    genBeginSponsoringFutureReservesResult, genClaimClaimableBalanceResult, genCreateAccountResult,
    genCreateClaimableBalanceResult, genEndSponsoringFutureReservesResult, genManageBuyOfferResult,
    genPathPaymentSendResult, genRevokeSponsorshipResult
  )

  def genBeginSponsoringFutureReservesResult: Gen[BeginSponsoringFutureReservesResult] = Gen.oneOf(
    BeginSponsoringFutureReservesSuccess, BeginSponsoringFutureReservesAlreadySponsored,
    BeginSponsoringFutureReservesMalformed, BeginSponsoringFutureReservesRecursive
  )

  def genEndSponsoringFutureReservesResult: Gen[EndSponsoringFutureReservesResult] = Gen.oneOf(
    EndSponsoringFutureReservesSuccess, EndSponsoringFutureReservesNotSponsored
  )

  def genRevokeSponsorshipResult: Gen[RevokeSponsorshipResult] = Gen.oneOf(
    RevokeSponsorshipSuccess, RevokeSponsorshipDoesNotExist, RevokeSponsorshipNotSponsor,
    RevokeSponsorshipOnlyTransferable
  )

  def genOperationNotAttemptedResult: Gen[OperationResult] =
    Gen.oneOf(BadAuthenticationResult, NoSourceAccountResult, OperationNotSupportedResult, TooManySubEntriesResult,
      ExceededWorkLimitResult, TooManySponsoringResult)

  def genAccountMergeResult: Gen[AccountMergeResult] = Gen.oneOf(
    genNativeAmount.map(AccountMergeSuccess), Gen.const(AccountMergeMalformed), Gen.const(AccountMergeNoAccount),
    Gen.const(AccountMergeImmutable), Gen.const(AccountMergeHasSubEntries), Gen.const(AccountMergeSeqNumTooFar),
    Gen.const(AccountMergeDestinationFull)
  )

  def genAllowTrustResult: Gen[AllowTrustResult] = Gen.oneOf(
    AllowTrustSuccess, AllowTrustCannotRevoke, AllowTrustMalformed, AllowTrustNotRequired, AllowTrustNoTrustLine,
    AllowTrustSelfNotAllowed
  )

  def genBumpSequenceResult: Gen[BumpSequenceResult] = Gen.oneOf(BumpSequenceSuccess, BumpSequenceBadSeqNo)

  def genChangeTrustResult: Gen[ChangeTrustResult] = Gen.oneOf(
    ChangeTrustSuccess, ChangeTrustInvalidLimit, ChangeTrustLowReserve, ChangeTrustMalformed,
    ChangeTrustNoIssuer, ChangeTrustSelfNotAllowed
  )

  def genCreateAccountResult: Gen[CreateAccountResult] = Gen.oneOf(
    CreateAccountSuccess, CreateAccountAlreadyExists, CreateAccountLowReserve, CreateAccountMalformed,
    CreateAccountUnderfunded
  )

  def genCreateClaimableBalanceResult: Gen[CreateClaimableBalanceResult] = Gen.oneOf(
    Gen.const(CreateClaimableBalanceMalformed), Gen.const(CreateClaimableBalanceLowReserve),
    Gen.const(CreateClaimableBalanceNoTrust), Gen.const(CreateClaimableBalanceNotAuthorized),
    Gen.const(CreateClaimableBalanceUnderfunded),
    ClaimableBalanceIds.genClaimableBalanceId.map(CreateClaimableBalanceSuccess)
  )

  def genClaimClaimableBalanceResult: Gen[ClaimClaimableBalanceResult] = Gen.oneOf(
    ClaimClaimableBalanceSuccess, ClaimClaimableBalanceDoesNotExist, ClaimClaimableBalanceCannotClaim,
    ClaimClaimableBalanceLineFull, ClaimClaimableBalanceNoTrust, ClaimClaimableBalanceNotAuthorized
  )

  def genCreatePassiveSellOfferResult: Gen[CreatePassiveSellOfferResult] = Gen.oneOf(
    genCreatePassiveSellOfferSuccess,
    Gen.const(CreatePassiveSellOfferMalformed),
    Gen.const(CreatePassiveSellOfferSellNoTrust),
    Gen.const(CreatePassiveSellOfferBuyNoTrust),
    Gen.const(CreatePassiveSellOfferSellNoAuth),
    Gen.const(CreatePassiveSellOfferBuyNoAuth),
    Gen.const(CreatePassiveSellOfferLineFull),
    Gen.const(CreatePassiveSellOfferUnderfunded),
    Gen.const(CreatePassiveSellOfferCrossSelf),
    Gen.const(CreatePassiveSellOfferSellNoIssuer),
    Gen.const(CreatePassiveSellOfferBuyNoIssuer),
    Gen.const(UpdatePassiveOfferIdNotFound),
    Gen.const(CreatePassiveSellOfferLowReserve)
  )

  def genCreatePassiveSellOfferSuccess: Gen[CreatePassiveSellOfferSuccess] = for {
    offer <- genOfferEntry
    claims <- Gen.listOf(genOfferClaim).map(_.take(20))
  } yield CreatePassiveSellOfferSuccess(offer, claims)

  def genInflationResult: Gen[InflationResult] = Gen.oneOf(
    Gen.listOf(genInflationPayout).map(InflationSuccess), Gen.const(InflationNotDue)
  )

  def genInflationPayout: Gen[InflationPayout] = for {
    recipient <- genPublicKey
    amount <- genNativeAmount
  } yield InflationPayout(recipient, amount)

  def genManageDataResult: Gen[ManageDataResult] = Gen.oneOf(
    ManageDataSuccess, ManageDataNotSupportedYet, DeleteDataNameNotFound, AddDataLowReserve, AddDataInvalidName
  )

  val genOfferEntry: Gen[OfferEntry] = for {
    account <- genPublicKey
    offerId <- Gen.posNum[Long]
    selling <- genAmount
    buying <- genAsset
    price <- genPrice
  } yield OfferEntry(account, offerId, selling, buying, price)

  def genManageBuyOfferSuccess: Gen[ManageBuyOfferSuccess] = for {
    claims <- Gen.listOf(genOfferClaim).map(_.take(20))
    entry <- genOfferEntry
    effect <- Gen.oneOf(ManageOfferEffect.values())
  } yield ManageBuyOfferSuccess(claims, Some(entry).filterNot(_ => effect == MANAGE_OFFER_DELETED), effect)

  def genManageSellOfferSuccess: Gen[ManageSellOfferSuccess] = for {
    claims <- Gen.listOf(genOfferClaim).map(_.take(20))
    entry <- genOfferEntry
    effect <- Gen.oneOf(ManageOfferEffect.values())
  } yield ManageSellOfferSuccess(claims, Some(entry).filterNot(_ => effect == MANAGE_OFFER_DELETED), effect)

  def genManageSellOfferResult: Gen[ManageSellOfferResult] = Gen.oneOf(
    genManageSellOfferSuccess,
    Gen.const(ManageSellOfferMalformed), Gen.const(ManageSellOfferBuyNoTrust), Gen.const(ManageSellOfferSellNoTrust),
    Gen.const(ManageSellOfferBuyNoAuth), Gen.const(ManageSellOfferSellNoAuth),
    Gen.const(ManageSellOfferBuyNoIssuer), Gen.const(ManageSellOfferSellNoIssuer),
    Gen.const(ManageSellOfferLineFull), Gen.const(ManageSellOfferUnderfunded), Gen.const(ManageSellOfferCrossSelf),
    Gen.const(ManageSellOfferLowReserve), Gen.const(UpdateSellOfferIdNotFound)
  )

  def genManageBuyOfferResult: Gen[ManageBuyOfferResult] = Gen.oneOf(
    genManageBuyOfferSuccess,
    Gen.const(ManageBuyOfferMalformed), Gen.const(ManageBuyOfferBuyNoTrust), Gen.const(ManageBuyOfferBuyNoTrust),
    Gen.const(ManageBuyOfferBuyNoAuth), Gen.const(ManageBuyOfferBuyNoAuth),
    Gen.const(ManageBuyOfferBuyNoIssuer), Gen.const(ManageBuyOfferBuyNoIssuer),
    Gen.const(ManageBuyOfferLineFull), Gen.const(ManageBuyOfferUnderfunded), Gen.const(ManageBuyOfferCrossSelf),
    Gen.const(ManageBuyOfferLowReserve), Gen.const(UpdateBuyOfferIdNotFound)
  )

  def genPathPaymentReceiveResult: Gen[PathPaymentReceiveResult] = Gen.oneOf(
    genPathPaymentReceiveSuccess,
    Gen.const(PathPaymentReceiveMalformed),
    Gen.const(PathPaymentReceiveUnderfunded),
    Gen.const(PathPaymentReceiveSourceNoTrust),
    Gen.const(PathPaymentReceiveSourceNotAuthorised),
    Gen.const(PathPaymentReceiveNoDestination),
    Gen.const(PathPaymentReceiveDestinationNoTrust),
    Gen.const(PathPaymentReceiveDestinationNotAuthorised),
    Gen.const(PathPaymentReceiveDestinationLineFull),
    genAsset.map(PathPaymentReceiveNoIssuer),
    Gen.const(PathPaymentReceiveTooFewOffers),
    Gen.const(PathPaymentReceiveOfferCrossesSelf),
    Gen.const(PathPaymentReceiveSendMaxExceeded)
  )

  def genPathPaymentReceiveSuccess: Gen[PathPaymentReceiveSuccess] = for {
    claims <- Gen.listOf(genOfferClaim)
    destination <- genPublicKey
    amount <- genAmount
  }  yield PathPaymentReceiveSuccess(claims, destination, amount)

  def genPathPaymentSendResult: Gen[PathPaymentSendResult] = Gen.oneOf(
    genPathPaymentSendSuccess,
    Gen.const(PathPaymentSendMalformed),
    Gen.const(PathPaymentSendUnderfunded),
    Gen.const(PathPaymentSendSourceNoTrust),
    Gen.const(PathPaymentSendSourceNotAuthorised),
    Gen.const(PathPaymentSendNoDestination),
    Gen.const(PathPaymentSendDestinationNoTrust),
    Gen.const(PathPaymentSendDestinationNotAuthorised),
    Gen.const(PathPaymentSendDestinationLineFull),
    genAsset.map(PathPaymentSendNoIssuer),
    Gen.const(PathPaymentSendTooFewOffers),
    Gen.const(PathPaymentSendOfferCrossesSelf),
    Gen.const(PathPaymentSendDestMinNotMet)
  )

  def genPathPaymentSendSuccess: Gen[PathPaymentSendSuccess] = for {
    claims <- Gen.listOf(genOfferClaim)
    destination <- genPublicKey
    amount <- genAmount
  }  yield PathPaymentSendSuccess(claims, destination, amount)

  def genPaymentResult: Gen[PaymentResult] = Gen.oneOf(
    PaymentSuccess,
    PaymentMalformed,
    PaymentUnderfunded,
    PaymentSourceNoTrust,
    PaymentSourceNotAuthorised,
    PaymentNoDestination,
    PaymentDestinationNoTrust,
    PaymentDestinationNotAuthorised,
    PaymentDestinationLineFull,
    PaymentNoIssuer
  )

  def genSetOptionsResult: Gen[SetOptionsResult] = Gen.oneOf(
    SetOptionsSuccess,
    SetOptionsLowReserve,
    SetOptionsTooManySigners,
    SetOptionsBadFlags,
    SetOptionsInvalidInflation,
    SetOptionsCannotChange,
    SetOptionsUnknownFlag,
    SetOptionsThresholdOutOfRange,
    SetOptionsBadSigner,
    SetOptionsInvalidHomeDomain
  )
  
  def genOfferClaim: Gen[OfferClaim] = for {
    seller <- genPublicKey
    offerId <- Gen.posNum[Long]
    sold <- genAmount
    bought <- genAmount
  } yield OfferClaim(seller, offerId, sold, bought)

  def genTransactionResult: Gen[TransactionResult] =
    Gen.oneOf(genTransactionSuccess, genTransactionFailure, genTransactionNotAttempted)

  def genTransactionSuccess: Gen[TransactionSuccess] = for {
    fee <- genNativeAmount
    opResults <- Gen.nonEmptyListOf(genOperationResult)
//    hash <- Gen.containerOfN[Array, Byte](32, Gen.posNum[Byte]).map(new ByteString(_))
  } yield TransactionSuccess(fee, opResults, ByteString.EMPTY)

  def genTransactionNotSuccessful: Gen[TransactionNotSuccessful] =
    Gen.oneOf(genTransactionFailure, genTransactionNotAttempted)

  def genTransactionFailure: Gen[TransactionFailure] = for {
    fee <- genNativeAmount
    opResults <- Gen.nonEmptyListOf(genOperationResult)
  } yield TransactionFailure(fee, opResults)

  def genTransactionNotAttempted: Gen[TransactionNotAttempted] = for {
    reason <- Gen.oneOf(TransactionResultCode.values().toSet -- Set(TransactionResultCode.txSUCCESS, TransactionResultCode.txFAILED))
    fee <- genNativeAmount
    innerResult <- genFeeBumpedTransactionResult.map(tr => reason match {
      case TransactionResultCode.txFEE_BUMP_INNER_SUCCESS | TransactionResultCode.txFEE_BUMP_INNER_FAILED => Some(tr)
      case _ => None
    })
  } yield TransactionNotAttempted(reason, fee, innerResult)

  def genFeeBumpedTransactionResult: Gen[FeeBumpedTransactionResult] = for {
    fee <- genNativeAmount
    result <- Gen.oneOf(TransactionResultCode.values().toSet --
      Set(TransactionResultCode.txFEE_BUMP_INNER_FAILED) - TransactionResultCode.txFEE_BUMP_INNER_SUCCESS)
    opResults <- result match {
      case TransactionResultCode.txFAILED | TransactionResultCode.txSUCCESS =>
        Gen.nonEmptyListOf(genOperationResult).map(_.take(20))
      case _ => Gen.const(List.empty[OperationResult])
    }
    hash <- genHash.map(ByteString.decodeBase64)
  } yield FeeBumpedTransactionResult(fee, result, opResults, hash)

  def genFeeStatsResponse: Gen[FeeStatsResponse] = for {
    lastLedger <- Gen.posNum[Long]
    lastLedgerBaseFee <- genNativeAmount
    ledgerCapacityUsage <- Gen.choose(0.0, 1.0)
    maxFees <- genFeeStats
    chargedFees <- genFeeStats
  } yield FeeStatsResponse(lastLedger, lastLedgerBaseFee, ledgerCapacityUsage, maxFees, chargedFees)

  def genFeeStats: Gen[FeeStats] = for {
    min <- genNativeAmount
    mode <- genNativeAmount
    max <- genNativeAmount
    percentiles <- Gen.listOfN(11, genNativeAmount).map(_.sortBy(_.units))
    percentilesMap = Seq(10, 20, 30, 40, 50, 60, 70 ,80, 90, 95, 99).zip(percentiles).toMap
  } yield FeeStats(min, mode, max, percentilesMap)

  def genNetworkInfo: Gen[NetworkInfo] = for {
    horizonVersion <- Gen.identifier
    coreVersion <- Gen.identifier
    earliestLedger <- Gen.choose(1, 99999)
    latestLedger <- Gen.choose(100000, 99999999999L)
    passphrase <- Arbitrary.arbString.arbitrary
    currentProtocolVersion <- Gen.choose(0, 100)
    supportedProtocolVersion <- Gen.choose(0, 100)
  } yield NetworkInfo(horizonVersion, coreVersion, earliestLedger, latestLedger, passphrase, currentProtocolVersion, supportedProtocolVersion)

  def genFederationResponse: Gen[FederationResponse] = for {
    name <- Gen.identifier
    account <- genPublicKey
    memo <- genMemo.suchThat(m => !m.isInstanceOf[MemoReturnHash])
  } yield FederationResponse(name, account, memo)

  def genPaymentPath: Gen[PaymentPath] = for {
    source <- genAmount
    destination <- genAmount
    path <- genAssetPath
  } yield PaymentPath(source, destination, path)

  // Not at all exhaustive.
  def genUri: Gen[HttpUrl] = for {
    scheme <- Gen.oneOf("http", "https")
    subDomain <- Gen.option(Gen.identifier.map(_.take(20) + "."))
    domain <- Gen.identifier.map(_.take(20))
    tld <- Gen.oneOf("com", "net", "io", "org")
    paths <- Gen.listOf(Gen.identifier.map(_.take(20))).map(_.take(5).mkString("/"))
    urlString = s"$scheme://${subDomain.getOrElse("")}$domain.$tld/$paths"
  } yield Option(HttpUrl.parse(urlString)).getOrElse(
    throw new IllegalStateException(s"""Could not parse - "$urlString"""")
  )

  def genPaymentSigningRequest: Gen[PaymentSigningRequest] = for {
    destination <- genPublicKey
    amount <- Gen.option(genAmount)
    memo <- genMemo
    callback <- Gen.option(genUri)
    message <- Gen.option(Gen.alphaNumStr.map(_.take(300)))
    network <- Gen.option(genNetwork)
    passphrase = network.map(_.passphrase)
    signature <- Gen.option(genDomainSignature)
  } yield PaymentSigningRequest(destination, amount, memo, callback, message, passphrase, signature)

  def genTransactionSigningRequest: Gen[TransactionSigningRequest] = for {
    network <- Gen.option(genNetwork)
    envelope: SignedTransaction <- genSignedTransaction(network.getOrElse(PublicNetwork))
    form <- Gen.mapOf(
      for {
        key <- Gen.identifier
        label <- Gen.alphaStr.suchThat(_.nonEmpty).suchThat(_.contains(":") != true)
        help <- Gen.alphaStr
      } yield (label, (key, help))
    )
    callback <- Gen.option(genUri)
    pubkey <- Gen.option(genPublicKey)
    message <- Gen.option(Gen.alphaNumStr.map(_.take(300)))
    passphrase = network.map(_.passphrase)
    signature <- Gen.option(genDomainSignature)
  } yield TransactionSigningRequest(envelope, form, callback, pubkey, message, passphrase, signature)

  def genDomainSignature: Gen[DomainSignature] = for {
    domain <- Gen.identifier
    signature <- genKeyPair.map(_.sign("anything".getBytes())).map(_.data).map(new ByteString(_))
  } yield DomainSignature(domain, signature)

  @tailrec
  final def sampleOne[T](genT: Gen[T]): T = genT.sample match {
    case None => sampleOne(genT)
    case Some(t) => t
  }
}

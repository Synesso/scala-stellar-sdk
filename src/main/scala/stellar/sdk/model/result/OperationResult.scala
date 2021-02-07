package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{OperationResultCode, OperationType, OperationResult => XOperationResult}
/**
 * The result of an operation previously submitted to the network.
 */
abstract class OperationResult {
  def xdr: XOperationResult
}

/**
 * The result of an operation previously submitted to, and attempted to be processed by the network.
 */
abstract class ProcessedOperationResult extends OperationResult {
  def transactionResult: OperationResultTr
  override def xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opINNER)
    .tr(transactionResult)
    .build()
}

object OperationResult {
  def decodeXdr(xdr: XOperationResult): OperationResult =
    xdr.getDiscriminant match {
      case OperationResultCode.opINNER => xdr.getTr.getDiscriminant match {
        case OperationType.CREATE_ACCOUNT => CreateAccountResult.decodeXdr(xdr.getTr.getCreateAccountResult)
        case OperationType.PAYMENT => PaymentResult.decodeXdr(xdr.getTr.getPaymentResult)
        case OperationType.PATH_PAYMENT_STRICT_RECEIVE => PathPaymentReceiveResult.decodeXdr(xdr.getTr.getPathPaymentStrictReceiveResult)
        case OperationType.MANAGE_SELL_OFFER => ManageSellOfferResult.decodeXdr(xdr.getTr.getManageSellOfferResult)
        case OperationType.CREATE_PASSIVE_SELL_OFFER => CreatePassiveSellOfferResult.decodeXdr(xdr.getTr.getCreatePassiveSellOfferResult)
        case OperationType.SET_OPTIONS => SetOptionsResult.decodeXdr(xdr.getTr.getSetOptionsResult)
        case OperationType.CHANGE_TRUST => ChangeTrustResult.decodeXdr(xdr.getTr.getChangeTrustResult)
        case OperationType.ALLOW_TRUST => AllowTrustResult.decodeXdr(xdr.getTr.getAllowTrustResult)
        case OperationType.ACCOUNT_MERGE => AccountMergeResult.decodeXdr(xdr.getTr.getAccountMergeResult)
        case OperationType.INFLATION => InflationResult.decodeXdr(xdr.getTr.getInflationResult)
        case OperationType.MANAGE_DATA => ManageDataResult.decodeXdr(xdr.getTr.getManageDataResult)
        case OperationType.BUMP_SEQUENCE => BumpSequenceResult.decodeXdr(xdr.getTr.getBumpSeqResult)
        case OperationType.MANAGE_BUY_OFFER => ManageBuyOfferResult.decodeXdr(xdr.getTr.getManageBuyOfferResult)
        case OperationType.PATH_PAYMENT_STRICT_SEND => PathPaymentSendResult.decodeXdr(xdr.getTr.getPathPaymentStrictSendResult)
        case OperationType.CREATE_CLAIMABLE_BALANCE => CreateClaimableBalanceResult.decodeXdr(xdr.getTr.getCreateClaimableBalanceResult)
        case OperationType.CLAIM_CLAIMABLE_BALANCE => ClaimClaimableBalanceResult.decodeXdr(xdr.getTr.getClaimClaimableBalanceResult)
        case OperationType.BEGIN_SPONSORING_FUTURE_RESERVES => BeginSponsoringFutureReservesResult.decodeXdr(xdr.getTr.getBeginSponsoringFutureReservesResult)
        case OperationType.END_SPONSORING_FUTURE_RESERVES => EndSponsoringFutureReservesResult.decodeXdr(xdr.getTr.getEndSponsoringFutureReservesResult)
        case OperationType.REVOKE_SPONSORSHIP => RevokeSponsorshipResult.decodeXdr(xdr.getTr.getRevokeSponsorshipResult)
      }
      case OperationResultCode.opBAD_AUTH => BadAuthenticationResult
      case OperationResultCode.opEXCEEDED_WORK_LIMIT => ExceededWorkLimitResult
      case OperationResultCode.opNO_ACCOUNT => NoSourceAccountResult
      case OperationResultCode.opNOT_SUPPORTED => OperationNotSupportedResult
      case OperationResultCode.opTOO_MANY_SPONSORING => TooManySponsoringResult
      case OperationResultCode.opTOO_MANY_SUBENTRIES => TooManySubEntriesResult
    }
}

/**
 * The operation was not attempted, because there were too few valid signatures, or the wrong network was used.
 */
case object BadAuthenticationResult extends OperationResult {
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opBAD_AUTH)
    .build()
}

/**
 * The operation was not attempted, because the source account was not found.
 */
case object NoSourceAccountResult extends OperationResult {
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opNO_ACCOUNT)
    .build()
}

/**
 * The operation was not attempted, because the requested operation is not supported by the network.
 */
case object OperationNotSupportedResult extends OperationResult {
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opNOT_SUPPORTED)
    .build()
}

/**
 * The operation was not attempted, because the maximum number of subentries was already reached.
 */
case object TooManySubEntriesResult extends OperationResult {
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opTOO_MANY_SUBENTRIES)
    .build()
}

/**
 * The operation was not attempted, because the operation did too much work.
 */
case object ExceededWorkLimitResult extends OperationResult {
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opEXCEEDED_WORK_LIMIT)
    .build()
}

/**
 * The operation was not attempted, because the account is sponsoring too many entries
 */
case object TooManySponsoringResult extends OperationResult {
  override val xdr: XOperationResult = new XOperationResult.Builder()
    .discriminant(OperationResultCode.opTOO_MANY_SPONSORING)
    .build()
}

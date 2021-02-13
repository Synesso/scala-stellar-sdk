package stellar.sdk.model.result

import org.stellar.xdr.OperationResult.OperationResultTr
import org.stellar.xdr.{ManageDataResultCode, OperationType, ManageDataResult => XManageDataResult}


sealed abstract class ManageDataResult extends ProcessedOperationResult {
  def result: XManageDataResult
  override def transactionResult: OperationResultTr = new OperationResultTr.Builder()
    .discriminant(OperationType.MANAGE_DATA)
    .manageDataResult(result)
    .build()
}

object ManageDataResult {
  def decodeXdr(xdr: XManageDataResult): ManageDataResult = xdr.getDiscriminant match {
    case ManageDataResultCode.MANAGE_DATA_SUCCESS => ManageDataSuccess
    case ManageDataResultCode.MANAGE_DATA_NOT_SUPPORTED_YET => ManageDataNotSupportedYet
    case ManageDataResultCode.MANAGE_DATA_NAME_NOT_FOUND => DeleteDataNameNotFound
    case ManageDataResultCode.MANAGE_DATA_LOW_RESERVE => AddDataLowReserve
    case ManageDataResultCode.MANAGE_DATA_INVALID_NAME => AddDataInvalidName
  }
}

/**
 * ManageData operation was successful.
 */
case object ManageDataSuccess extends ManageDataResult {
  override def result: XManageDataResult = new XManageDataResult.Builder()
    .discriminant(ManageDataResultCode.MANAGE_DATA_SUCCESS)
    .build()
}

/**
 * ManageData operation failed because the network was not yet prepared to support this operation.
 */
case object ManageDataNotSupportedYet extends ManageDataResult {
  override def result: XManageDataResult = new XManageDataResult.Builder()
    .discriminant(ManageDataResultCode.MANAGE_DATA_NOT_SUPPORTED_YET)
    .build()
}

/**
 * ManageData operation failed because there was no data entry with the given name.
 */
case object DeleteDataNameNotFound extends ManageDataResult {
  override def result: XManageDataResult = new XManageDataResult.Builder()
    .discriminant(ManageDataResultCode.MANAGE_DATA_NAME_NOT_FOUND)
    .build()
}

/**
 * ManageData operation failed because there was insufficient reserve to support the addition of a new data entry.
 */
case object AddDataLowReserve extends ManageDataResult {
  override def result: XManageDataResult = new XManageDataResult.Builder()
    .discriminant(ManageDataResultCode.MANAGE_DATA_LOW_RESERVE)
    .build()
}

/**
 * ManageData operation failed because the name was not a valid string.
 */
// TODO - all the failure scenarios need to stop masquerading as ProcessedOperationResult
//  otherwise xdr.getTr.getDiscriminant is NPE
case object AddDataInvalidName extends ManageDataResult {
  override def result: XManageDataResult = new XManageDataResult.Builder()
    .discriminant(ManageDataResultCode.MANAGE_DATA_INVALID_NAME)
    .build()
}
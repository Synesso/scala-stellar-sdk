package stellar

import stellar.sdk.model.response.AccountResponse

import scala.language.implicitConversions

package object sdk {

  implicit def accnFromAccnResp(resp: AccountResponse) = resp.toAccount

}

package stellar

import stellar.sdk.response.AccountResp

import scala.language.implicitConversions

package object sdk {

  implicit def accnFromAccnResp(resp: AccountResp) = resp.toAccount

}

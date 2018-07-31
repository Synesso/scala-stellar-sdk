package stellar

import stellar.sdk.resp.AccountResp
import scala.language.implicitConversions

package object sdk {

  implicit def accnFromAccnResp(resp: AccountResp) = resp.toAccount

}

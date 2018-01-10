package stellar.scala.sdk

import org.specs2.mutable.Specification

class PathPaymentOperationSpec extends Specification with ArbitraryInput with DomainMatchers {

  "path payment operation" should {
    "serde via xdr" >> prop {
      (source: KeyPair, destination: VerifyingKey, sendMax: Amount, destinationAmount: Amount, path: Seq[Asset]) =>

        val input = PathPaymentOperation(source, sendMax, destination, destinationAmount, path)
        val triedOperation = Operation.fromXDR(input.toXDR)
        if (triedOperation.isFailure) throw triedOperation.failed.get
        triedOperation must beSuccessfulTry.like {
          case ppo: PathPaymentOperation =>
            ppo.destinationAccount.accountId mustEqual destination.accountId
            ppo.sendMax must beEquivalentTo(sendMax)
            ppo.destinationAmount must beEquivalentTo(destinationAmount)
            ppo.path must haveSize(path.size)
            if (ppo.path.nonEmpty) {
              ppo.path.zip(path).map { case (l, r) => l must beEquivalentTo(r) }.reduce(_ and _)
            }
            ppo.sourceAccount must beNone
        }
    }.setGen5(genAssetPath)
  }

}

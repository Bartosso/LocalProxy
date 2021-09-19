package forex.services.rates

import derevo.derive
import tofu.logging.derivation.loggable

object errors {

  @derive(loggable)
  sealed trait Error
  object Error {
    final case class NoValueForKey(key: String) extends Error
    case object FromAndToAreTheSame extends Error
  }

}

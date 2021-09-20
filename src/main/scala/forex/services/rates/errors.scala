package forex.services.rates

import derevo.derive
import forex.domain.CacheKey
import tofu.logging.derivation.loggable

object errors {

  @derive(loggable)
  sealed trait Error
  object Error {
    final case class NoValueForKey(key: CacheKey) extends Error
    case object FromAndToAreTheSame extends Error
  }

}

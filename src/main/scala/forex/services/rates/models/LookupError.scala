package forex.services.rates.models

import derevo.derive
import forex.domain.CacheKey
import tofu.logging.derivation.loggable

@derive(loggable)
sealed trait LookupError
object LookupError {
  final case class NoValueForKey(key: CacheKey) extends LookupError
  case object FromAndToAreTheSame extends LookupError
}

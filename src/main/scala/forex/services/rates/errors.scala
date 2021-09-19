package forex.services.rates

import derevo.derive
import tofu.logging.derivation.loggable

object errors {

  @derive(loggable)
  sealed trait Error
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
  }

}

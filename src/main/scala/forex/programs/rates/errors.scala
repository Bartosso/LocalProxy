package forex.programs.rates

import forex.programs.rates.errors.Error._
import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends Exception
  object Error {
    case object RateLookupUnreachable extends Error
    case object MeaninglessRequest extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.NoValueForKey(_)    => RateLookupUnreachable
    case RatesServiceError.FromAndToAreTheSame => MeaninglessRequest
  }
}

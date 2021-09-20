package forex.programs.rates.models

import forex.programs.rates.models.RatesRequestError.{ MeaninglessRequest, RateLookupUnreachable }
import forex.services.rates.errors.{ Error => RatesServiceError }

case object Converters {
  def toProgramError(error: RatesServiceError): RatesRequestError = error match {
    case RatesServiceError.NoValueForKey(_)    => RateLookupUnreachable
    case RatesServiceError.FromAndToAreTheSame => MeaninglessRequest
  }
}

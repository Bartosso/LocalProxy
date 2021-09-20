package forex.programs.rates.models

import forex.programs.rates.models.RatesRequestError.{ MeaninglessRequest, RateLookupUnreachable }
import forex.services.rates.models.LookupError
import forex.services.rates.models.LookupError.{ FromAndToAreTheSame, NoValueForKey }

case object Converters {
  def toProgramError(error: LookupError): RatesRequestError = error match {
    case NoValueForKey(_)    => RateLookupUnreachable
    case FromAndToAreTheSame => MeaninglessRequest
  }
}

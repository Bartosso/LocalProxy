package forex.programs.rates.models

sealed trait RatesRequestError extends Exception
object RatesRequestError {
  case object RateLookupUnreachable extends RatesRequestError
  case object MeaninglessRequest extends RatesRequestError
}

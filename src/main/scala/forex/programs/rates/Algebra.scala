package forex.programs.rates

import forex.domain.Rate
import forex.programs.rates.models.{ GetRatesRequest, RatesRequestError }

trait Algebra[F[_]] {
  def get(request: GetRatesRequest): F[RatesRequestError Either Rate]
}

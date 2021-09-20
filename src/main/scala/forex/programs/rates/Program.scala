package forex.programs.rates

import cats.Functor
import cats.data.EitherT
import forex.domain._
import forex.programs.rates.models.Converters.toProgramError
import forex.programs.rates.models.{ GetRatesRequest, RatesRequestError }
import forex.services.RatesService

class Program[F[_]: Functor](
    ratesService: RatesService[F]
) extends Algebra[F] {

  override def get(request: GetRatesRequest): F[RatesRequestError Either Rate] =
    EitherT(ratesService.get(Rate.Pair(request.from, request.to))).leftMap(toProgramError).value

}

object Program {

  def apply[F[_]: Functor](
      ratesService: RatesService[F]
  ): Algebra[F] = new Program[F](ratesService)

}

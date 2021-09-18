package forex.services.rates.interpreters

import forex.domain.Rate
import forex.services.rates.{Algebra, errors}

class OneFrameREST[F[_]] extends Algebra[F] {
  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] = ???
}

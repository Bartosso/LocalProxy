package forex.services.rates.interpreters

import forex.services.rates.OneFrameAlgebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.models.LookupError

class OneFrameDummy[F[_]: Applicative] extends OneFrameAlgebra[F] {

  override def get(pair: Rate.Pair): F[LookupError Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[LookupError].pure[F]

}

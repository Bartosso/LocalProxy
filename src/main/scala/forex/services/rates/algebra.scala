package forex.services.rates

import forex.domain.Rate
import errors._

trait OneFrameAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
}

trait CacheUpdateAlgebra[F[_]] {
  def start(): F[Unit]
}

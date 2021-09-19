package forex.services.rates

import cats.effect.Resource
import forex.domain.Rate
import errors._
import scalacache.AbstractCache

trait OneFrameAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
}

trait CacheSynchronizationAlgebra[F[_]] {
  def start(): Resource[F, AbstractCache[Rate]]
}

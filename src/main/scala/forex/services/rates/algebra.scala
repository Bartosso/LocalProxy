package forex.services.rates

import cats.effect.Resource
import forex.domain.{ CacheKey, Rate }
import forex.services.rates.models.LookupError

trait OneFrameAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[LookupError Either Rate]
}

trait CacheSynchronizationAlgebra[F[_]] {
  def start(): Resource[F, Unit]
}

trait CacheAlgebra[F[_]] {
  def put(in: Rate): F[Unit]
  def get(in: CacheKey): F[Option[Rate]]
}

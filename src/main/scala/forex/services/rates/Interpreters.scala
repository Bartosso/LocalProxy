package forex.services.rates

import cats.effect.Resource
import cats.{ Applicative, MonadThrow }
import forex.domain.Rate
import interpreters._
import scalacache.{ AbstractCache, Mode }
import tofu.logging.Logs

object Interpreters {
  def dummy[F[_]: Applicative]: OneFrameAlgebra[F] = new OneFrameDummy[F]()
  def cachedImpl[F[_]: MonadThrow: Mode](cache: AbstractCache[Rate],
                                         logs: Logs[F, F]): Resource[F, OneFrameAlgebra[F]] =
    OneFrameCachedImpl[F](cache, logs)
}

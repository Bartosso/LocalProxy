package forex.services.rates

import cats.effect.Resource
import cats.{ Applicative, MonadThrow }
import interpreters._
import tofu.logging.Logs

object Interpreters {
  def dummy[F[_]: Applicative]: OneFrameAlgebra[F] = new OneFrameDummy[F]()
  def cachedImpl[F[_]: MonadThrow](cache: CacheAlgebra[F], logs: Logs[F, F]): Resource[F, OneFrameAlgebra[F]] =
    OneFrameCachedImpl[F](cache, logs)
}

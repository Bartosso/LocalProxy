package forex.services.rates

import cats.effect.{ Concurrent, Resource, Timer }
import cats.Applicative
import forex.config.OneFrameConfig
import forex.http.rates.client.algebra.OneFrameHttpClient
import interpreters._
import scalacache.Mode
import tofu.logging.Logs

object Interpreters {
  def dummy[F[_]: Applicative]: OneFrameAlgebra[F] = new OneFrameDummy[F]()
  def cachedImpl[F[_]: Timer: Mode: Concurrent](client: OneFrameHttpClient[F],
                                                config: OneFrameConfig,
                                                logs: Logs[F, F]): Resource[F, OneFrameAlgebra[F]] =
    OneFrameCachedImpl[F](client, config, logs)
}

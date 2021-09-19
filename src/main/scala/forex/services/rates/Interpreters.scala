package forex.services.rates

import cats.effect.{ Concurrent, Resource, Timer }
import cats.Applicative
import forex.config.OneFrameConfig
import forex.http.rates.client.OneFrameHttpClientImpl
import interpreters._
import scalacache.Mode

object Interpreters {
  def dummy[F[_]: Applicative]: OneFrameAlgebra[F] = new OneFrameDummy[F]()
  def httpTarget[F[_]: Timer: Mode: Concurrent](client: OneFrameHttpClientImpl[F],
                                                config: OneFrameConfig): Resource[F, OneFrameAlgebra[F]] =
    OneFrameImpl[F](client, config)
}

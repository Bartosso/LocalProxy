package forex.services.rates

import cats.{ Applicative, Functor }
import forex.http.rates.client.OneFrameHttpClientImpl
import interpreters._

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F]                         = new OneFrameDummy[F]()
  def httpTarget[F[_]: Functor](client: OneFrameHttpClientImpl[F]) = new OneFrameImpl[F](client)
}

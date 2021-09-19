package forex.services.rates.interpreters

import cats.MonadThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.Resource
import forex.domain.Rate
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.{ errors, OneFrameAlgebra }
import forex.services.rates.Utils._
import scalacache.{ AbstractCache, Mode }
import tofu.logging.{ Logs, ServiceLogging }
import tofu.syntax.logging._

private class OneFrameCachedImpl[F[_]: MonadThrow: Mode: ServiceLogging[*[_], OneFrameAlgebra[F]]](
    cache: AbstractCache[Rate]
) extends OneFrameAlgebra[F] {

  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] = {
    val key = pair.toKeyString
    debug"getting $key from the cache" *> cache
      .get(key)
      .recoverWith { err =>
        errorCause"Cache is empty" (err).as(None)
      }
      .map[Either[errors.Error, Rate]](_.toRight(OneFrameLookupFailed("Got no value")))
      .flatTap(
        res =>
          res.fold(
            err => error"can't get $key from the cache, error - $err",
            _ => debug"getting $key from the cache successfully done"
        )
      )
  }
}

object OneFrameCachedImpl {
  def apply[F[_]: MonadThrow: Mode](cache: AbstractCache[Rate], logs: Logs[F, F]): Resource[F, OneFrameAlgebra[F]] =
    Resource
      .eval(logs.service[OneFrameAlgebra[F]])
      .map(implicit logs => new OneFrameCachedImpl(cache))
}

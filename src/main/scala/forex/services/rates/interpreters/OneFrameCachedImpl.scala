package forex.services.rates.interpreters

import cats.MonadThrow
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.effect.Resource
import forex.domain.{ CacheKey, Rate }
import forex.services.rates.errors.Error.{ FromAndToAreTheSame, NoValueForKey }
import forex.services.rates.{ errors, OneFrameAlgebra }
import forex.services.rates.errors.Error
import scalacache.{ AbstractCache, Mode }
import tofu.logging.{ Logs, ServiceLogging }
import tofu.syntax.logging._

final class OneFrameCachedImpl[F[_]: MonadThrow: Mode: ServiceLogging[*[_], OneFrameAlgebra[F]]](
    cache: AbstractCache[Rate]
) extends OneFrameAlgebra[F] {

  private def getKayAndCutMeaningless(pair: Rate.Pair): Either[Error, CacheKey] =
    if (pair.from == pair.to) FromAndToAreTheSame.asLeft[CacheKey]
    else pair.toCacheKey.asRight[Error]

  private def getByKey(cacheKey: CacheKey): F[Either[Error, Rate]] =
    debug"getting $cacheKey from the cache" >> cache
      .get(cacheKey)
      .recoverWith { err =>
        // Somehow if caffeine is used and there is no value - I got error
        errorCause"Cache is empty" (err).as(None)
      }
      .map[Either[errors.Error, Rate]](_.toRight(NoValueForKey(cacheKey)))
      .flatTap(
        _.fold(
          err => error"can't get $cacheKey from the cache, error - $err",
          _ => debug"getting $cacheKey from the cache successfully done"
        )
      )

  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] = {
    val maybeKey = getKayAndCutMeaningless(pair)
    maybeKey.flatTraverse(getByKey)
  }
}

object OneFrameCachedImpl {
  def apply[F[_]: MonadThrow: Mode](cache: AbstractCache[Rate], logs: Logs[F, F]): Resource[F, OneFrameAlgebra[F]] =
    Resource
      .eval(logs.service[OneFrameAlgebra[F]])
      .map(implicit logs => new OneFrameCachedImpl(cache))
}

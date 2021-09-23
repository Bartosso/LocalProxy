package forex.services.rates.interpreters

import cats.MonadThrow
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.effect.Resource
import forex.domain.{ CacheKey, Rate }
import forex.services.rates.{ CacheAlgebra, OneFrameAlgebra }
import forex.services.rates.models.LookupError
import forex.services.rates.models.LookupError.{ FromAndToAreTheSame, NoValueForKey }
import tofu.logging.{ Logs, ServiceLogging }
import tofu.syntax.logging._

final class OneFrameCachedImpl[F[_]: MonadThrow: ServiceLogging[*[_], OneFrameAlgebra[F]]](
    cache: CacheAlgebra[F]
) extends OneFrameAlgebra[F] {

  private def getKayAndCutMeaningless(pair: Rate.Pair): LookupError Either CacheKey =
    if (pair.from == pair.to) FromAndToAreTheSame.asLeft[CacheKey]
    else pair.toCacheKey.asRight[LookupError]

  private def getByKey(cacheKey: CacheKey): F[LookupError Either Rate] =
    debug"getting $cacheKey from the cache" >> cache
      .get(cacheKey)
      .map[LookupError Either Rate](_.toRight(NoValueForKey(cacheKey)))
      .flatTap(
        _.fold(
          err => error"can't get $cacheKey from the cache, error - $err",
          _ => debug"getting $cacheKey from the cache successfully done"
        )
      )

  override def get(pair: Rate.Pair): F[LookupError Either Rate] = {
    val maybeKey = getKayAndCutMeaningless(pair)
    maybeKey.flatTraverse(getByKey)
  }
}

object OneFrameCachedImpl {
  def apply[F[_]: MonadThrow](cache: CacheAlgebra[F], logs: Logs[F, F]): Resource[F, OneFrameAlgebra[F]] =
    Resource
      .eval(logs.service[OneFrameAlgebra[F]])
      .map(implicit logs => new OneFrameCachedImpl(cache))
}

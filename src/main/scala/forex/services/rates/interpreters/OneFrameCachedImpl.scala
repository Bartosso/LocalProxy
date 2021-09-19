package forex.services.rates.interpreters

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.instances.list._
import cats.effect.syntax.concurrent._
import cats.data.NonEmptyList
import cats.effect.{ Concurrent, Resource, Timer }
import com.github.benmanes.caffeine.cache.Caffeine
import forex.config.{ CacheConfig, OneFrameConfig }
import forex.domain.{ Currency, Rate }
import forex.http.rates.client.Protocol.Out.{ GetCurrenciesRequest, GetCurrencyValuePair }
import forex.http.rates.client.algebra.OneFrameHttpClient
import forex.http.rates.client.errors.OneFrameHttpClientError
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.{ errors, OneFrameAlgebra }
import forex.services.rates.Utils._
import forex.services.rates.interpreters.OneFrameCachedImpl.allPairs
import scalacache._
import scalacache.caffeine.CaffeineCache
import scalacache.redis._
import scalacache.serialization.circe._
import scalacache.{ AbstractCache, Mode }
import tofu.logging.{ Loggable, Logs, ServiceLogging }
import tofu.syntax.logging._

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

private class OneFrameCachedImpl[F[_]: Timer: Monad: Mode: ServiceLogging[*[_], OneFrameAlgebra[F]]](
    cli: OneFrameHttpClient[F],
    cache: AbstractCache[Rate],
    cacheTtl: FiniteDuration,
    refreshRate: FiniteDuration
) extends OneFrameAlgebra[F] {

  private val updateCacheFun: F[Unit] =
    info"Starting update of the cache values" >>
      cli
        .getCurrenciesRates(GetCurrenciesRequest(allPairs))
        .flatMap(
          _.fold(
            err => error"Can't update cache values, error - $err",
            result =>
              result.toList
                .traverse { value =>
                  val key = value.toKeyString
                  cache.put(key)(value.toRate, Some(cacheTtl))
                } >> info"Cache update successfully done"
          )
        )

  private val updateCacheLoop: F[Unit] = (Timer[F].sleep(refreshRate) >> updateCacheFun).foreverM[Unit]

  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] = {
    val key = pair.toKeyString
    cache.get(key).map[Either[errors.Error, Rate]](_.toRight(OneFrameLookupFailed("Got no value")))
  }
}

object OneFrameCachedImpl {

  private val maybeAllPairs = for {
    from <- Currency.values
    to <- Currency.values
    result <- Option.when(from != to)(GetCurrencyValuePair(from, to))
  } yield result

  private val allPairs = NonEmptyList.fromListUnsafe(maybeAllPairs.toList)

  def apply[F[_]: Timer: Mode: Concurrent](cli: OneFrameHttpClient[F],
                                           config: OneFrameConfig,
                                           logs: Logs[F, F]): Resource[F, OneFrameAlgebra[F]] = {
    val cache       = initCache(config.cacheConfig)
    val cacheTtl    = config.cacheConfig.ttl
    val refreshRate = calculateRefreshRate(cacheTtl)
    for {
      impl <- Resource
               .eval(logs.service[OneFrameAlgebra[F]])
               .map(implicit logs => new OneFrameCachedImpl(cli, cache, refreshRate, cacheTtl))
      _ <- impl.updateCacheLoop.background
    } yield impl
  }

  private def calculateRefreshRate(cacheTtl: FiniteDuration): FiniteDuration =
    if (cacheTtl <= 5.seconds) 1.second else cacheTtl - 5.seconds

  private def initCache[F[_]](cacheConfig: CacheConfig): AbstractCache[Rate] = {
    val maybeRedisCache: Option[AbstractCache[Rate]] = for {
      host <- cacheConfig.redisHost
      port <- cacheConfig.redisPort
    } yield RedisCache[Rate](host, port)

    maybeRedisCache.getOrElse {
      val size = allPairs.size.toLong
      val underlyingCaffeineCache =
        Caffeine
          .newBuilder()
          .maximumSize(size)
          .expireAfterWrite(cacheConfig.ttl.length, cacheConfig.ttl.unit)
          .build[String, Entry[Rate]]
      CaffeineCache(underlyingCaffeineCache)
    }
  }
}

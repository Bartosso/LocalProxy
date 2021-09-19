package forex.services.rates.interpreters

import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.applicative._
import cats.syntax.traverse._
import cats.instances.list._
import cats.effect.syntax.concurrent._
import cats.data.NonEmptyList
import cats.effect.{ Concurrent, Resource, Timer }
import com.github.benmanes.caffeine.cache.Caffeine
import forex.config.{ CacheConfig, OneFrameConfig }
import forex.domain.{ Currency, Rate }
import forex.http.rates.client.OneFrameHttpClientImpl
import forex.http.rates.client.Protocol.Out.{ GetCurrenciesRequest, GetCurrencyValuePair }
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.rates.{ errors, OneFrameAlgebra }
import forex.services.rates.Utils._
import forex.services.rates.interpreters.OneFrameImpl.allPairs
import scalacache._
import scalacache.caffeine.CaffeineCache
import scalacache.redis._
import scalacache.serialization.circe._
import scalacache.{ AbstractCache, Mode }

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

private class OneFrameImpl[F[_]: Timer: Monad: Mode](cli: OneFrameHttpClientImpl[F],
                                                     cache: AbstractCache[Rate],
                                                     cacheTtl: FiniteDuration,
                                                     refreshRate: FiniteDuration)
    extends OneFrameAlgebra[F] {

  private val updateCacheFun: F[Unit] =
    cli
      .getCurrenciesRates(GetCurrenciesRequest(allPairs))
      .flatMap(
        _.fold(
          _ => ().pure[F],
          result =>
            result.toList
              .traverse { value =>
                val key = value.toKeyString
                cache.put(key)(value.toRate, Some(cacheTtl))
              }
              .as(())
        )
      )

  val updateCacheLoop: F[Unit] = (updateCacheFun >> Timer[F].sleep(refreshRate)).foreverM[Unit]

  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] = {
    val key = pair.toKeyString
    cache.get(key).map[Either[errors.Error, Rate]](_.toRight(OneFrameLookupFailed("Got no value")))
  }
}

object OneFrameImpl {

  private val maybeAllPairs = for {
    from <- Currency.values
    to <- Currency.values
    result <- Option.when(from != to)(GetCurrencyValuePair(from, to))
  } yield result

  private val allPairs = NonEmptyList.fromListUnsafe(maybeAllPairs.toList)

  def apply[F[_]: Timer: Mode: Concurrent](cli: OneFrameHttpClientImpl[F],
                                           config: OneFrameConfig): Resource[F, OneFrameAlgebra[F]] = {
    val cache       = initCache(config.cacheConfig)
    val cacheTtl    = config.cacheConfig.ttl
    val refreshRate = calculateRefreshRate(cacheTtl)
    val impl        = new OneFrameImpl(cli, cache, refreshRate, cacheTtl)
    for {
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

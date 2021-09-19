package forex.services.rates.interpreters

import cats.effect.{ Concurrent, Resource, Timer }
import cats.effect.syntax.concurrent._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.instances.list._
import cats.data.NonEmptyList
import cats.implicits.catsSyntaxApplicativeId
import com.github.benmanes.caffeine.cache.Caffeine
import forex.config.{ CacheConfig, OneFrameConfig }
import forex.domain.{ Currency, Rate, Timestamp }
import forex.http.rates.client.Protocol.Out.{ GetCurrenciesRequest, GetCurrencyValuePair }
import forex.http.rates.client.algebra.OneFrameHttpClient
import forex.services.rates.Utils.GetCurrenciesValueOps
import forex.services.rates.interpreters.CacheSynchronizationImpl.allPairs
import forex.services.rates.{ CacheSynchronizationAlgebra, OneFrameAlgebra }
import scalacache.caffeine.CaffeineCache
import scalacache.redis.RedisCache
import scalacache.serialization.circe._
import scalacache.{ AbstractCache, Entry, Mode }
import tofu.logging.{ Logs, ServiceLogging }
import tofu.syntax.logging._

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

private class CacheSynchronizationImpl[F[_]: Timer: Concurrent: Mode: ServiceLogging[*[_], OneFrameAlgebra[
  F
]]](
    cache: AbstractCache[Rate],
    oneFrameClient: OneFrameHttpClient[F],
    cacheTtl: FiniteDuration,
    refreshRate: FiniteDuration
) extends CacheSynchronizationAlgebra[F] {

  private val updateCacheFun: F[Unit] =
    info"Starting update of the cache values" >>
      oneFrameClient
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

  private def updateCacheIfItsOld(actualRate: Rate): F[Unit] = {
    val now      = Timestamp.now
    val rateTime = actualRate.timestamp
    // If we take default limit 1000 requests per day - we can perform a request every 86.4 seconds, soo 100 should be enough.
    val threshold           = cacheTtl / 3
    val latestSyncThreshold = now.value.minusSeconds(threshold.toSeconds)
    if (rateTime.value.isBefore(latestSyncThreshold)) updateCacheFun
    else ().pure[F]
  }

  override def start(): Resource[F, AbstractCache[Rate]] = {
    val synchronizationInit =
      info"starting cache synchronization" >> cache
        .get(allPairs.head.toKeyString)
        .recoverWith { err =>
          // Somehow if caffeine is used and there is no value - I got error
          errorCause"Cache is empty" (err).as(None)
        }
        .flatMap {
          _.fold(updateCacheFun)(updateCacheIfItsOld)
        }
    Resource.eval(synchronizationInit) >> updateCacheLoop.background.as(cache)
  }
}

object CacheSynchronizationImpl {

  def createSyncedCache[F[_]: Timer: Mode: Concurrent](cli: OneFrameHttpClient[F],
                                                       config: OneFrameConfig,
                                                       logs: Logs[F, F]): Resource[F, AbstractCache[Rate]] = {
    val cache       = initCache(config.cacheConfig)
    val cacheTtl    = config.cacheConfig.ttl
    val refreshRate = calculateRefreshRate(cacheTtl)
    for {
      impl <- Resource.eval(
               logs
                 .service[OneFrameAlgebra[F]]
                 .map(implicit logs => new CacheSynchronizationImpl(cache, cli, refreshRate, cacheTtl))
             )
      _ <- impl.start()
    } yield cache
  }

  private val maybeAllPairs = for {
    from <- Currency.values
    to <- Currency.values
    result <- Option.when(from != to)(GetCurrencyValuePair(from, to))
  } yield result

  private val allPairs = NonEmptyList.fromListUnsafe(maybeAllPairs.toList)

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

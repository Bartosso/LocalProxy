package forex.services.rates.interpreters

import cats.ApplicativeThrow
import cats.effect.Resource
import cats.syntax.applicativeError._
import cats.syntax.functor._
import com.github.benmanes.caffeine.cache.Caffeine
import forex.config.CacheConfig
import forex.domain.{ CacheKey, Currency, Rate }
import forex.services.rates.CacheAlgebra
import scalacache.caffeine.CaffeineCache
import scalacache.redis.RedisCache
import scalacache.serialization.circe.codec
import scalacache.{ AbstractCache, Entry, Mode }
import tofu.logging.{ Logs, ServiceLogging }
import tofu.syntax.logging._

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

class CacheImpl[F[_]: Mode: ApplicativeThrow: ServiceLogging[*[_], CacheAlgebra[F]]](cache: AbstractCache[Rate],
                                                                                     cacheTtl: FiniteDuration)
    extends CacheAlgebra[F] {
  private val someCacheTtl = Some(cacheTtl)

  override def put(in: Rate): F[Unit] = {
    val key = in.pair.toCacheKey
    cache.put(key.value)(in, someCacheTtl).recoverWith {
      case NonFatal(e) => errorCause"can't put $key to the cache" (e).as(())
    }
  }.as(())

  override def get(in: CacheKey): F[Option[Rate]] =
    cache.get(in.value).recoverWith {
      case NonFatal(e) => errorCause"can't get $in from the cache" (e).as(None)
    }
}

object CacheImpl {
  def resource[F[_]: Mode: ApplicativeThrow](cacheConfig: CacheConfig, logs: Logs[F, F]): Resource[F, CacheImpl[F]] = {
    val maybeRedisCache: Option[AbstractCache[Rate]] = for {
      host <- cacheConfig.redisHost
      port <- cacheConfig.redisPort
    } yield RedisCache[Rate](host, port)

    val actualCache = maybeRedisCache.getOrElse {
      val size = Currency.allPairs.size.toLong
      val underlyingCaffeineCache =
        Caffeine
          .newBuilder()
          .maximumSize(size)
          .expireAfterWrite(cacheConfig.ttl.length, cacheConfig.ttl.unit)
          .build[String, Entry[Rate]]
      CaffeineCache(underlyingCaffeineCache)
    }

    Resource.eval(logs.service[CacheAlgebra[F]]).map(implicit logs => new CacheImpl[F](actualCache, cacheConfig.ttl))
  }
}

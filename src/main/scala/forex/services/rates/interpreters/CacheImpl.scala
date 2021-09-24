package forex.services.rates.interpreters

import cats.{ ApplicativeThrow, MonadThrow }
import cats.effect.Resource
import cats.implicits.catsSyntaxFlatMapOps
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

/** Just a wrapper, since scala cache have an ugly API (key is Any*, requires to have Mode type-class at every call).
  * Can contain Redis cache or Caffeine if a user didn't set up redis-host and redis-port in the config.
  */
class CacheImpl[F[_]: Mode: ApplicativeThrow: ServiceLogging[*[_], CacheAlgebra[F]]](cache: AbstractCache[Rate],
                                                                                     cacheTtl: FiniteDuration)
    extends CacheAlgebra[F] {
  private val someCacheTtl = Some(cacheTtl)

  override def put(in: Rate): F[Unit] = {
    val key = in.pair.toCacheKey
    cache.put(key.value)(in, someCacheTtl).recoverWith {
      case NonFatal(e) => errorCause"can't put $key to the cache" (e).void.widen
    }
  }.void

  override def get(in: CacheKey): F[Option[Rate]] =
    cache.get(in.value).recoverWith {
      case NonFatal(e) => errorCause"can't get $in from the cache" (e).as(None)
    }
}

object CacheImpl {
  def resource[F[_]: Mode: MonadThrow](cacheConfig: CacheConfig, logs: Logs[F, F]): Resource[F, CacheImpl[F]] =
    Resource.eval(logs.service[CacheAlgebra[F]]).flatMap { implicit logger =>
      val maybeRedisCache: Option[AbstractCache[Rate]] = for {
        host <- cacheConfig.redisHost
        port <- cacheConfig.redisPort
      } yield RedisCache[Rate](host, port)

      val redisOrCaffeineCache = maybeRedisCache.toRight {
        val size = Currency.allPairs.size.toLong
        val underlyingCaffeineCache =
          Caffeine
            .newBuilder()
            .maximumSize(size)
            .expireAfterWrite(cacheConfig.ttl.length, cacheConfig.ttl.unit)
            .build[String, Entry[Rate]]
        CaffeineCache(underlyingCaffeineCache)
      }

      Resource.eval(
        info"Initializing cache" >> redisOrCaffeineCache.fold(
          caffeineCache => info"Using in-memory caffeine cache".as(new CacheImpl[F](caffeineCache, cacheConfig.ttl)),
          redisCache => info"Using redis cache".as(new CacheImpl[F](redisCache, cacheConfig.ttl))
        )
      )
    }
}

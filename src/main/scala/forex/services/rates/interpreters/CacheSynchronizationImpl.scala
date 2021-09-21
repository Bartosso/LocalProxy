package forex.services.rates.interpreters

import cats.effect.{ Concurrent, Resource, Timer }
import cats.effect.syntax.concurrent._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.instances.list._
import cats.data.NonEmptyList
import cats.implicits.catsSyntaxApplicativeId
import forex.config.CacheConfig
import forex.domain.{ Currency, Rate, Timestamp }
import forex.http.rates.client.models.in._
import forex.http.rates.client.models._
import forex.http.rates.client.algebra.OneFrameHttpClient
import forex.http.rates.client.models
import forex.http.rates.client.models.OneFrameHttpClientError.ClientError
import forex.http.rates.server.models.Converters.GetCurrencyValuePairOps
import forex.services.rates.{ CacheAlgebra, CacheSynchronizationAlgebra }
import tofu.logging.{ Logs, ServiceLogging }
import tofu.syntax.logging._

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

final class CacheSynchronizationImpl[F[_]: Timer: Concurrent: ServiceLogging[*[_], CacheSynchronizationAlgebra[F]]](
    cache: CacheAlgebra[F],
    oneFrameClient: OneFrameHttpClient[F],
    refreshRate: FiniteDuration
) extends CacheSynchronizationAlgebra[F] {

  private val allPairsAsGetCurrencyValuePair = Currency.allPairs.map(_.asGetCurrencyValuePair)

  private val updateCacheFun: F[Unit] =
    info"Starting update of the cache values" >> oneFrameClient
      .getCurrenciesRates(models.GetCurrenciesRequest(allPairsAsGetCurrencyValuePair))
      .flatMap(handleClientResult)

  private val updateCacheLoop: F[Unit] = (Timer[F].sleep(refreshRate) >> updateCacheFun).foreverM[Unit]

  private val synchronizationInit =
    info"Startup cache synchronisation" >> cache
      .get(Currency.allPairs.head.toCacheKey)
      .flatMap(_.fold(updateCacheFun)(updateCacheIfItsOld))

  private def handleClientResult(
      in: Either[OneFrameHttpClientError, NonEmptyList[GetCurrencyValue]]
  ): F[Unit] =
    in.fold(logClientError, updateCacheWithValues)

  private def updateCacheWithValues(values: NonEmptyList[GetCurrencyValue]): F[Unit] =
    values.toList.traverse(value => cache.put(value.toRate)) >> info"Cache update successfully done"

  private def logClientError: PartialFunction[OneFrameHttpClientError, F[Unit]] = {
    case ClientError(error) => errorCause"Can't update cache values, client error" (error)
    case error              => error"Can't update cache values, error - $error"
  }

  private def updateCacheIfItsOld(actualRate: Rate): F[Unit] = {
    val now                 = Timestamp.now
    val rateTime            = actualRate.timestamp
    val latestSyncThreshold = now.value.minusSeconds(refreshRate.toSeconds)
    if (rateTime.value.isBefore(latestSyncThreshold)) updateCacheFun
    else ().pure[F]
  }

  override def start(): Resource[F, Unit] =
    Resource.eval(synchronizationInit) >> updateCacheLoop.background.as(())
}

object CacheSynchronizationImpl {

  def startCacheSynchronization[F[_]: Timer: Concurrent](cli: OneFrameHttpClient[F],
                                                         cache: CacheAlgebra[F],
                                                         config: CacheConfig,
                                                         logs: Logs[F, F]): Resource[F, Unit] = {
    val refreshRate = calculateRefreshRate(config.ttl)
    for {
      impl <- Resource.eval(
               logs
                 .service[CacheSynchronizationAlgebra[F]]
                 .map(implicit logs => new CacheSynchronizationImpl(cache, cli, refreshRate))
             )
      _ <- impl.start()
    } yield ()
  }

  private def calculateRefreshRate(cacheTtl: FiniteDuration): FiniteDuration =
    if (cacheTtl <= 3.microseconds) 1.millisecond
    // If we take the default limit is 1000 requests per day - we can perform a request every 86.4 seconds
    // so 100 seconds should be enough
    else cacheTtl / 3
}

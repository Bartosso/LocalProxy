package forex.services.rates.interpreters

import cats.data.NonEmptyList
import cats.effect.{ ContextShift, IO, Timer }
import cats.syntax.applicative._
import cats.syntax.either._
import forex.Generators
import forex.domain.{ CacheKey, Currency, Price, Rate, Timestamp }
import forex.http.rates.client.models.in.GetCurrencyValue
import forex.http.rates.client.models.{ GetCurrenciesRequest, OneFrameHttpClientError }
import forex.http.rates.client.algebra.OneFrameHttpClient
import forex.services.rates.{ CacheAlgebra, CacheSynchronizationAlgebra }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scalacache.CatsEffect.modes.async
import scalacache.caffeine.CaffeineCache
import tofu.logging.{ Logs, ServiceLogging }

import java.time.OffsetDateTime
import java.util.concurrent.Executors
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

class CacheSynchronizationImplSpec extends AnyWordSpec with Matchers {
  implicit val blockingEC: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))
  implicit lazy val context: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO]               = IO.timer(blockingEC)

  val testLogs: Logs[IO, IO]      = Logs.sync[IO, IO]
  val cacheTtl: FiniteDuration    = 5.minutes
  val refreshRate: FiniteDuration = 100.seconds

  implicit val cacheLogs: ServiceLogging[IO, CacheAlgebra[IO]] = testLogs.service[CacheAlgebra[IO]].unsafeRunSync()
  implicit val cacheSyncLogs: ServiceLogging[IO, CacheSynchronizationAlgebra[IO]] =
    testLogs.service[CacheSynchronizationAlgebra[IO]].unsafeRunSync()

  "CacheSynchronizationImpl" should {
    "sync cache on the start if the cache is empty" in {
      val testCache      = new CacheImpl[IO](CaffeineCache[Rate], cacheTtl)
      val someCurrency   = Generators.getCurrencyValue()
      val dummyClient    = createDummyClientWithFun(NonEmptyList.one(someCurrency).asRight)
      val dummyCacheSync = new CacheSynchronizationImpl[IO](testCache, dummyClient, refreshRate)
      val key            = CacheKey(someCurrency.from, someCurrency.to)
      val expectedValue  = someCurrency.toRate

      testCache.get(someCurrency.toCacheKey).unsafeRunSync().isEmpty shouldBe true

      val release = dummyCacheSync.start().allocated.unsafeRunSync()._2

      testCache.get(key).unsafeRunSync() shouldBe Some(expectedValue)
      release.unsafeRunSync()
      succeed
    }

    "don't sync cache if head is fresh" in {
      val testCache = new CacheImpl[IO](CaffeineCache[Rate], cacheTtl)
      // We assume that first pair is AUD and CAD since enumeratum provides indexed seq to us
      val headPair = (Currency.values(0), Currency.values(1))
      val freshCurrency =
        Generators.getCurrencyValue().copy(timeStamp = Timestamp.now, from = headPair._1, to = headPair._2)
      val veryFreshCurrency =
        Generators.getCurrencyValue(1L).copy(timeStamp = Timestamp.now, from = headPair._1, to = headPair._2)

      val dummyClient    = createDummyClientWithFun(NonEmptyList.one(veryFreshCurrency).asRight)
      val dummyCacheSync = new CacheSynchronizationImpl[IO](testCache, dummyClient, refreshRate)

      val key           = CacheKey(freshCurrency.from, freshCurrency.to)
      val expectedValue = freshCurrency.toRate

      testCache.put(expectedValue).unsafeRunSync()

      val release = dummyCacheSync.start().allocated.unsafeRunSync()._2

      testCache.get(key).unsafeRunSync() shouldBe Some(expectedValue)
      release.unsafeRunSync()
      succeed
    }

    "sync cache if head value is outdated" in {
      val outdatedDuration = refreshRate + 1.seconds
      val testCache        = new CacheImpl[IO](CaffeineCache[Rate], cacheTtl)
      // We assume that first pair is AUD and CAD since enumeratum provides indexed seq to us
      val headPair                 = (Currency.values(0), Currency.values(1))
      val exceedThresholdTimestamp = Timestamp.now.copy(OffsetDateTime.now().minusSeconds(outdatedDuration.toSeconds))
      val exceedThresholdCurrencyValue =
        Generators.getCurrencyValue().copy(timeStamp = exceedThresholdTimestamp, from = headPair._1, to = headPair._2)
      val veryFreshCurrency =
        Generators.getCurrencyValue(1L).copy(timeStamp = Timestamp.now, from = headPair._1, to = headPair._2)

      val dummyClient    = createDummyClientWithFun(NonEmptyList.one(veryFreshCurrency).asRight)
      val dummyCacheSync = new CacheSynchronizationImpl[IO](testCache, dummyClient, refreshRate)

      val exceedRefreshRateValue = exceedThresholdCurrencyValue.toRate
      val expectedFreshValue     = veryFreshCurrency.toRate

      testCache.put(exceedRefreshRateValue).unsafeRunSync()

      val release = dummyCacheSync.start().allocated.unsafeRunSync()._2

      testCache.get(exceedRefreshRateValue.pair.toCacheKey).unsafeRunSync() shouldBe Some(expectedFreshValue)
      release.unsafeRunSync()
      succeed
    }

    "replace old value with the new one after some time" in {
      val testCache               = new CacheImpl[IO](CaffeineCache[Rate], cacheTtl)
      val currencyValueAtTheStart = Generators.getCurrencyValue()
      val currencyUpdated         = currencyValueAtTheStart.copy(timeStamp = Timestamp.now, price = Price(90L))
      val funRefreshRate          = 3.milliseconds

      val dummyClient: OneFrameHttpClient[IO] = new OneFrameHttpClient[IO] {
        var evidence = false
        override def getCurrenciesRates(
            in: GetCurrenciesRequest
        ): IO[Either[OneFrameHttpClientError, NonEmptyList[GetCurrencyValue]]] =
          if (!evidence) {
            evidence = true
            NonEmptyList.one(currencyValueAtTheStart).asRight[OneFrameHttpClientError].pure[IO]
          } else { NonEmptyList.one(currencyUpdated).asRight[OneFrameHttpClientError].pure[IO] }
      }
      val dummyCacheSync = new CacheSynchronizationImpl[IO](testCache, dummyClient, funRefreshRate)
      val key            = currencyValueAtTheStart.toCacheKey
      val expectedValue  = currencyUpdated.toRate

      testCache.get(key).unsafeRunSync().isEmpty shouldBe true

      val release = dummyCacheSync.start().allocated.unsafeRunSync()._2

      Timer[IO].sleep(funRefreshRate * 10).unsafeRunSync()

      testCache.get(key).unsafeRunSync() shouldBe Some(expectedValue)
      release.unsafeRunSync()
      succeed
    }
  }

  def createDummyClientWithFun(
      fun: => Either[OneFrameHttpClientError, NonEmptyList[GetCurrencyValue]]
  ): OneFrameHttpClient[IO] = (_: GetCurrenciesRequest) => IO(fun)

}

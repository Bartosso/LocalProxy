package forex.services.rates.interpreters

import cats.data.NonEmptyList
import cats.effect.{ ContextShift, IO, Timer }
import cats.syntax.either._
import forex.Generators
import forex.Util.getCurrencyValueToRate
import forex.domain.{ Currency, Rate, Timestamp }
import forex.http.rates.client.Protocol.In.GetCurrencyValue
import forex.http.rates.client.Protocol.Out
import forex.http.rates.client.algebra.OneFrameHttpClient
import forex.http.rates.client.errors.OneFrameHttpClientError
import forex.services.rates.CacheSynchronizationAlgebra
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scalacache.CatsEffect.modes.async
import scalacache.modes.sync._
import scalacache.caffeine.CaffeineCache
import tofu.logging.{ Logs, ServiceLogging }

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

class CacheSynchronizationImplSpec extends AnyWordSpec with Matchers {
  implicit val contextShit: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO]              = IO.timer(ExecutionContext.global)

  val testLogs: Logs[IO, IO]      = Logs.sync[IO, IO]
  val cacheTtl: FiniteDuration    = 5.minutes
  val refreshRate: FiniteDuration = 100.seconds

  implicit val cacheSyncLogs: ServiceLogging[IO, CacheSynchronizationAlgebra[IO]] =
    testLogs.service[CacheSynchronizationAlgebra[IO]].unsafeRunSync()

  "CacheSynchronizationImpl" should {
    "sync cache on the start if the cache is empty" in {
      val testCache      = CaffeineCache[Rate]
      val someCurrency   = Generators.getCurrencyValue()
      val dummyClient    = createDummyClient(NonEmptyList.one(someCurrency).asRight)
      val dummyCacheSync = new CacheSynchronizationImpl[IO](testCache, dummyClient, cacheTtl, refreshRate)
      val key            = someCurrency.from.entryName + someCurrency.to.entryName
      val expectedValue  = getCurrencyValueToRate(someCurrency)

      testCache.get(key).isEmpty shouldBe true

      val release = dummyCacheSync.start().allocated.unsafeRunSync()._2

      testCache.get(key) shouldBe Some(expectedValue)
      release.unsafeRunSync()
      succeed
    }

    "don't sync cache if head is fresh" in {
      val testCache = CaffeineCache[Rate]
      // We assume that first pair is AUD and CAD since enumeratum provides indexed seq to us
      val headPair = (Currency.values(0), Currency.values(1))
      val freshCurrency =
        Generators.getCurrencyValue().copy(timeStamp = Timestamp.now, from = headPair._1, to = headPair._2)
      val veryFreshCurrency =
        Generators.getCurrencyValue(1L).copy(timeStamp = Timestamp.now, from = headPair._1, to = headPair._2)

      val dummyClient    = createDummyClient(NonEmptyList.one(veryFreshCurrency).asRight)
      val dummyCacheSync = new CacheSynchronizationImpl[IO](testCache, dummyClient, cacheTtl, refreshRate)

      val key           = freshCurrency.from.entryName + freshCurrency.to.entryName
      val expectedValue = getCurrencyValueToRate(freshCurrency)

      testCache.put(key)(expectedValue)

      val release = dummyCacheSync.start().allocated.unsafeRunSync()._2

      testCache.get(key) shouldBe Some(expectedValue)
      release.unsafeRunSync()
      succeed
    }

    "sync cache if head value is outdated" in {
      val outdatedDuration = refreshRate + 1.seconds
      val testCache        = CaffeineCache[Rate]
      // We assume that first pair is AUD and CAD since enumeratum provides indexed seq to us
      val headPair                 = (Currency.values(0), Currency.values(1))
      val exceedThresholdTimestamp = Timestamp.now.copy(OffsetDateTime.now().minusSeconds(outdatedDuration.toSeconds))
      val exceedThresholdCurrencyValue =
        Generators.getCurrencyValue().copy(timeStamp = exceedThresholdTimestamp, from = headPair._1, to = headPair._2)
      val veryFreshCurrency =
        Generators.getCurrencyValue(1L).copy(timeStamp = Timestamp.now, from = headPair._1, to = headPair._2)

      val dummyClient    = createDummyClient(NonEmptyList.one(veryFreshCurrency).asRight)
      val dummyCacheSync = new CacheSynchronizationImpl[IO](testCache, dummyClient, cacheTtl, refreshRate)

      val key                    = exceedThresholdCurrencyValue.from.entryName + exceedThresholdCurrencyValue.to.entryName
      val exceedRefreshRateValue = getCurrencyValueToRate(exceedThresholdCurrencyValue)
      val expectedFreshValue     = getCurrencyValueToRate(veryFreshCurrency)

      testCache.put(key)(exceedRefreshRateValue)

      val release = dummyCacheSync.start().allocated.unsafeRunSync()._2

      testCache.get(key) shouldBe Some(expectedFreshValue)
      release.unsafeRunSync()
      succeed
    }

  }

  def createDummyClient(
      response: Either[OneFrameHttpClientError, NonEmptyList[GetCurrencyValue]]
  ): OneFrameHttpClient[IO] =
    (_: Out.GetCurrenciesRequest) => IO.pure(response)

}

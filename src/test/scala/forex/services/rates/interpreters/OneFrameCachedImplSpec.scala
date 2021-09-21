package forex.services.rates.interpreters

import cats.effect.IO
import cats.syntax.either._
import forex.Generators
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Rate }
import forex.services.rates.CacheAlgebra
import forex.services.rates.interpreters.OneFrameCachedImpl
import forex.services.rates.models.LookupError.{ FromAndToAreTheSame, NoValueForKey }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scalacache.CatsEffect.modes.async
import scalacache.caffeine.CaffeineCache
import tofu.logging.{ Logs, ServiceLogging }

import scala.concurrent.duration.DurationInt

class OneFrameCachedImplSpec extends AnyWordSpec with Matchers {
  val testLogs: Logs[IO, IO] = Logs.sync[IO, IO]

  implicit val cacheLogs: ServiceLogging[IO, CacheAlgebra[IO]] = testLogs.service[CacheAlgebra[IO]].unsafeRunSync()

  "OneFrameCachedImpl" should {
    "work fine when cache have the value" in {
      val testCache     = new CacheImpl[IO](CaffeineCache[Rate], 10.minutes)
      val insertedValue = Generators.rate()
      testCache.put(insertedValue).unsafeRunSync()

      val mustBeRight = OneFrameCachedImpl[IO](testCache, testLogs)
        .use(algebra => algebra.get(insertedValue.pair))
        .unsafeRunSync()
      mustBeRight shouldBe insertedValue.asRight
    }

    "return error if there is no value in the cache" in {
      val testCache = new CacheImpl[IO](CaffeineCache[Rate], 10.minutes)
      val someValue = Generators.rate()

      val mustBeLeft = OneFrameCachedImpl[IO](testCache, testLogs)
        .use(algebra => algebra.get(someValue.pair))
        .unsafeRunSync()
      mustBeLeft shouldBe NoValueForKey(someValue.pair.toCacheKey).asLeft
    }

    "return an error if the field `from` and `to` in the pair is the same" in {
      val testCache = new CacheImpl[IO](CaffeineCache[Rate], 10.minutes)

      val mustBeLeft = OneFrameCachedImpl[IO](testCache, testLogs)
        .use(algebra => algebra.get(Pair(Currency.CAD, Currency.CAD)))
        .unsafeRunSync()
      mustBeLeft shouldBe FromAndToAreTheSame.asLeft
    }
  }

}

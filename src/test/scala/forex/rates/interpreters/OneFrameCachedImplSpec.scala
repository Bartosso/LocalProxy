package forex.rates.interpreters

import cats.effect.IO
import cats.syntax.either._
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.rates.Utils.PairOps
import forex.services.rates.errors.Error.{ FromAndToAreTheSame, NoValueForKey }
import forex.services.rates.interpreters.OneFrameCachedImpl
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scalacache.CatsEffect.modes.async
import scalacache.modes.sync._
import scalacache.caffeine.CaffeineCache
import tofu.logging.Logs

class OneFrameCachedImplSpec extends AnyWordSpec with Matchers {
  val testLogs = Logs.sync[IO, IO]

  "OneFrameCachedImpl" should {
    "work fine when cache have the value" in {
      val testCache     = CaffeineCache[Rate]
      val insertedValue = buildSomeRate()
      testCache.put(insertedValue.pair.toKeyString)(insertedValue)

      val mustBeRight = OneFrameCachedImpl[IO](testCache, testLogs)
        .use(algebra => algebra.get(insertedValue.pair))
        .unsafeRunSync()
      mustBeRight shouldBe insertedValue.asRight
    }

    "return error if there is no value in the cache" in {
      val testCache = CaffeineCache[Rate]
      val someValue = buildSomeRate()

      val mustBeLeft = OneFrameCachedImpl[IO](testCache, testLogs)
        .use(algebra => algebra.get(someValue.pair))
        .unsafeRunSync()
      mustBeLeft shouldBe NoValueForKey(someValue.pair.toKeyString).asLeft
    }

    "return an error if the field `from` and `to` in the pair is the same" in {
      val testCache = CaffeineCache[Rate]

      val mustBeLeft = OneFrameCachedImpl[IO](testCache, testLogs)
        .use(algebra => algebra.get(Pair(Currency.CAD, Currency.CAD)))
        .unsafeRunSync()
      mustBeLeft shouldBe FromAndToAreTheSame.asLeft
    }
  }

  private def buildSomeRate(from: Currency = Currency.CAD, to: Currency = Currency.AUD) = {
    val pair = Pair(from, to)
    Rate(pair, Price(BigDecimal(342)), Timestamp.now)
  }

}

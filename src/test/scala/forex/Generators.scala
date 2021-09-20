package forex

import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.domain.Rate.Pair
import forex.http.rates.client.Protocol.In.GetCurrencyValue
import org.scalacheck._

import java.time.ZoneOffset

object Generators {

  def generateCurrency: Gen[Currency] = Gen.oneOf(Currency.values)

  def generatePair: Gen[Pair] =
    for {
      from <- generateCurrency
      to <- generateCurrency
    } yield Pair(from, to)

  def generateNotBrokenPair: Gen[Pair] = generatePair.suchThat(pair => pair.from != pair.to)

  def generatePrice: Gen[Price] = Gen.posNum[BigDecimal].map(Price.apply)

  //Let's assume we are in UTC
  def generateTimeStamp: Gen[Timestamp] =
    Gen.calendar.map(_.getTime.toInstant.atOffset(ZoneOffset.UTC)).map(Timestamp.apply)

  def generateRate: Gen[Rate] =
    for {
      pair <- generateNotBrokenPair
      price <- generatePrice
      timeStamp <- generateTimeStamp
    } yield Rate(pair, price, timeStamp)

  def rate(seed: Long = 42L): Rate = generateRate.pureApply(Gen.Parameters.default, rng.Seed(seed))

  def genGetCurrencyValue: Gen[GetCurrencyValue] =
    for {
      pair <- generateNotBrokenPair
      bid <- Gen.posNum[BigDecimal]
      ask <- Gen.posNum[BigDecimal]
      price <- generatePrice
      timeStamp <- generateTimeStamp
    } yield GetCurrencyValue(pair.from, pair.to, bid, ask, price, timeStamp)

  def getCurrencyValue(seed: Long = 42L): GetCurrencyValue =
    genGetCurrencyValue.pureApply(Gen.Parameters.default, rng.Seed(seed))

}

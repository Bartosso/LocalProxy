package forex.http.rates.client.models.in

import forex.domain.Rate.Pair
import forex.domain.{ CacheKey, Currency, Price, Rate, Timestamp }
import forex.domain.Utils._
import forex.http.rates.client.models.JsonConfig._
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

final case class GetCurrencyValue(from: Currency,
                                  to: Currency,
                                  bid: BigDecimal,
                                  ask: BigDecimal,
                                  price: Price,
                                  timeStamp: Timestamp) {
  def toCacheKey: CacheKey = CacheKey(from, to)
  def toRate: Rate = {
    val pair = Pair(from, to)
    Rate(pair, price, timeStamp)
  }
}

object GetCurrencyValue {
  implicit val getCurrenciesValueDecoder: Decoder[GetCurrencyValue] =
    deriveConfiguredDecoder[GetCurrencyValue]

}

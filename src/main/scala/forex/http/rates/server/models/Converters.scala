package forex.http.rates.server.models

import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.http.rates.client.models.out.GetCurrencyValuePair
import forex.http.rates.server.models.out.GetApiResponse

object Converters {
  private[rates] implicit class GetApiResponseOps(val rate: Rate) extends AnyVal {
    def asGetApiResponse: GetApiResponse =
      GetApiResponse(
        from = rate.pair.from,
        to = rate.pair.to,
        price = rate.price,
        timestamp = rate.timestamp
      )
  }
  implicit class GetCurrencyValuePairOps(val pair: Pair) extends AnyVal {
    def asGetCurrencyValuePair: GetCurrencyValuePair = GetCurrencyValuePair(pair.from, pair.to)
  }
}

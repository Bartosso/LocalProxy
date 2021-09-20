package forex.http.rates.server.models

import forex.domain.Rate
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
}

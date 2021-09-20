package forex.http.rates.client.algebra

import cats.data.NonEmptyList
import forex.http.rates.client.models.{ GetCurrenciesRequest, OneFrameHttpClientError }
import forex.http.rates.client.models.in.GetCurrencyValue

trait OneFrameHttpClient[F[_]] {
  def getCurrenciesRates(in: GetCurrenciesRequest): F[OneFrameHttpClientError Either NonEmptyList[GetCurrencyValue]]
}

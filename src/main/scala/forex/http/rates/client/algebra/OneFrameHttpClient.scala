package forex.http.rates.client.algebra

import cats.data.NonEmptyList
import forex.http.rates.client.Protocol.In.GetCurrencyValue
import forex.http.rates.client.Protocol.Out.GetCurrenciesRequest
import forex.http.rates.client.errors.OneFrameHttpClientError

trait OneFrameHttpClient[F[_]] {
  def getCurrenciesRates(in: GetCurrenciesRequest): F[OneFrameHttpClientError Either NonEmptyList[GetCurrencyValue]]
}

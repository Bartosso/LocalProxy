package forex.http.rates.clients

import forex.http.rates.clients.Protocol.In.GetCurrenciesResponse
import forex.http.rates.clients.Protocol.Out.GetCurrenciesRequest
import forex.http.rates.clients.errors.OneFrameRestClientErrors

sealed trait OneFrameRestClient[F[_]] {
  def getCurrencies(in: GetCurrenciesRequest): F[OneFrameRestClientErrors Either GetCurrenciesResponse]
}

object OneFrameRestClient {}

package forex.http.rates.client.models

import cats.data.NonEmptyList
import forex.http.rates.client.models.out.GetCurrencyValuePair

final case class GetCurrenciesRequest(currencies: NonEmptyList[GetCurrencyValuePair])

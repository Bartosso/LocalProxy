package forex.http.rates.clients

import cats.data.NonEmptyList
import forex.domain.Currency

object Protocol {

  object Out {
    final case class GetCurrenciesRequest(currencies: List[Currency])
  }

  object In {
    final case class GetCurrenciesResponse(response: NonEmptyList[GetCurrenciesValue])

    final case class GetCurrenciesValue(from: Currency,
                                        to: Currency,
                                        bid: BigDecimal,
                                        ask: BigDecimal,
                                        price: BigDecimal,
                                        timeStamp: BigDecimal)
  }

}

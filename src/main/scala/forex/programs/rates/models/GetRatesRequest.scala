package forex.programs.rates.models

import forex.domain.Currency

final case class GetRatesRequest(from: Currency, to: Currency)

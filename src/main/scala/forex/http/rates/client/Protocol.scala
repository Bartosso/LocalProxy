package forex.http.rates.client

import cats.data.NonEmptyList
import forex.domain.{ Currency, Timestamp }
import forex.http.rates.Utils._
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import org.http4s.{ QueryParamEncoder, QueryParameterValue }

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  object Out {
    final case class GetCurrencyValuePair(from: Currency, to: Currency)
    final case class GetCurrenciesRequest(currencies: NonEmptyList[GetCurrencyValuePair])

    implicit val queryEncoder: QueryParamEncoder[GetCurrencyValuePair] = (value: GetCurrencyValuePair) =>
      QueryParameterValue(s"${value.from.entryName}${value.to.entryName}")
  }

  object In {
    final case class GetCurrenciesValue(from: Currency,
                                        to: Currency,
                                        bid: BigDecimal,
                                        ask: BigDecimal,
                                        price: BigDecimal,
                                        timeStamp: Timestamp)

    implicit lazy val getCurrenciesValueDecoder: Decoder[GetCurrenciesValue] =
      deriveConfiguredDecoder[GetCurrenciesValue]
  }
}
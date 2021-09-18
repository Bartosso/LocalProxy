package forex.http
package rates

import cats.data.NonEmptyList
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{ deriveConfiguredDecoder, deriveConfiguredEncoder }
import org.http4s.{ QueryParamEncoder, QueryParameterValue }

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiRequest(
      from: Currency,
      to: Currency
  )

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  final case class ParseCurrencyError(field: String, message: String)

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

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { in =>
      Json.fromString(in.entryName)
    }

  implicit val pairEncoder: Encoder[Pair] =
    deriveConfiguredEncoder[Pair]

  implicit val rateEncoder: Encoder[Rate] =
    deriveConfiguredEncoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]

  implicit val unknownCurrencyEncoder: Encoder[ParseCurrencyError] =
    deriveConfiguredEncoder[ParseCurrencyError]

}

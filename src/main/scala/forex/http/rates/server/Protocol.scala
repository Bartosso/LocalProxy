package forex.http.rates.server

import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.domain.Utils._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.{ Encoder, Json }

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  final case class ParseCurrencyError(field: String, message: String)

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

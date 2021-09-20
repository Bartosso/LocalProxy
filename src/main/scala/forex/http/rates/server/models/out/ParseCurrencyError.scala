package forex.http.rates.server.models.out

import forex.http.rates.server.models.JsonConfig._
import forex.domain.Utils._
import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

final case class ParseCurrencyError(field: String, message: String)

object ParseCurrencyError {
  implicit val unknownCurrencyEncoder: Encoder[ParseCurrencyError] = deriveConfiguredEncoder[ParseCurrencyError]
}

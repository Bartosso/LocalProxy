package forex.http.rates.client.models

import io.circe.generic.extras.Configuration

object JsonConfig {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
}

package forex.domain

import io.circe._
import io.circe.generic.semiauto._
import forex.domain.Utils._

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  implicit val rateEncoder: Encoder[Rate] = deriveEncoder[Rate]
  implicit val rateDecoder: Decoder[Rate] = deriveDecoder[Rate]

  implicit val pairEncoder: Encoder[Pair] = deriveEncoder[Pair]
  implicit val pairDecoder: Decoder[Pair] = deriveDecoder[Pair]
}

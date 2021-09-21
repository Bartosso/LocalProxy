package forex.domain

import io.circe._
import io.circe.generic.semiauto._
import forex.domain.Utils._
import tofu.logging.{ DictLoggable, LogRenderer }
import tofu.syntax.monoid.TofuSemigroupOps

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  ) {
    def toCacheKey: CacheKey = CacheKey(this)
  }

  implicit val pairLoggable: DictLoggable[Pair] = new DictLoggable[Pair] {
    override def fields[I, V, R, S](a: Pair, i: I)(implicit r: LogRenderer[I, V, R, S]): R =
      r.addString("from", a.from.entryName, i) |+| r.addString("to", a.to.entryName, i)

    override def logShow(a: Pair): String = s"Pair{from=${a.from.entryName} to=${a.to.entryName}}"
  }

  implicit val rateEncoder: Encoder[Rate] = deriveEncoder[Rate]
  implicit val rateDecoder: Decoder[Rate] = deriveDecoder[Rate]

  implicit val pairEncoder: Encoder[Pair] = deriveEncoder[Pair]
  implicit val pairDecoder: Decoder[Pair] = deriveDecoder[Pair]
}

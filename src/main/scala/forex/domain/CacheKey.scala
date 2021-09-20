package forex.domain

import derevo.derive
import forex.domain.Rate.Pair
import tofu.logging.derivation.loggable

@derive(loggable)
case class CacheKey(value: String) extends AnyVal

object CacheKey {
  def apply(in: Pair): CacheKey = CacheKey(in.from.entryName + in.to.entryName)

  def apply(from: Currency, to: Currency): CacheKey = CacheKey(from.entryName + to.entryName)
}

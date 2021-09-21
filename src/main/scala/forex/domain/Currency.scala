package forex.domain

import cats.data.NonEmptyList
import enumeratum._
import forex.domain.Rate.Pair

sealed trait Currency extends EnumEntry.Uppercase

object Currency extends Enum[Currency] with CirceEnum[Currency] {
  val values: IndexedSeq[Currency] = findValues

  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency

  val allPairsIndexed: IndexedSeq[Pair] = for {
    from <- Currency.values
    to <- Currency.values
    result <- Option.when(from != to)(Pair(from, to))
  } yield result

  val allPairs: NonEmptyList[Pair] = NonEmptyList.fromListUnsafe(allPairsIndexed.toList)

}

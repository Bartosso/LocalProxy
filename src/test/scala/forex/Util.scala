package forex

import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }

object Util {

  def buildSomeRate(from: Currency = Currency.CAD, to: Currency = Currency.AUD): Rate = {
    val pair = Pair(from, to)
    Rate(pair, Price(BigDecimal(342)), Timestamp.now)
  }

}

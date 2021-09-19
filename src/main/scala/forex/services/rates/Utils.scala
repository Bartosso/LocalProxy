package forex.services.rates

import forex.domain.Rate
import forex.domain.Rate.Pair
import forex.http.rates.client.Protocol.In.GetCurrencyValue

object Utils {

  implicit class PairOps(private val in: Pair) extends AnyVal {
    def toKeyString: String = s"${in.from.entryName}${in.to.entryName}"
  }

  implicit class GetCurrenciesValueOps(private val in: GetCurrencyValue) extends AnyVal {
    def toKeyString: String = s"${in.from.entryName}${in.to.entryName}"
    def toRate: Rate = {
      val pair = Pair(in.from, in.to)
      Rate(pair, in.price, in.timeStamp)
    }
  }

}

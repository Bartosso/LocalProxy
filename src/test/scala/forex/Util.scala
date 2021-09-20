package forex

import forex.domain.Rate.Pair
import forex.domain.Rate
import forex.http.rates.client.models.in.GetCurrencyValue

object Util {

  def getCurrencyValueToRate(in: GetCurrencyValue): Rate = {
    val pair = Pair(in.from, in.to)
    Rate(pair, in.price, in.timeStamp)
  }

}

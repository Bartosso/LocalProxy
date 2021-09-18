package forex.http
package rates

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.syntax.apply._
import cats.syntax.flatMap._
import forex.domain.Currency
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.{ HttpRoutes, ParseFailure }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      parseGetRatesRequest(from, to).fold(
        parsingErrors => BadRequest(parsingErrors),
        request =>
          rates.get(request).flatMap(Sync[F].fromEither).flatMap { rate =>
            Ok(rate.asGetApiResponse)
        }
      )
  }

  private def parseGetRatesRequest(
      maybeFrom: ValidatedNel[ParseFailure, Currency],
      maybeTo: ValidatedNel[ParseFailure, Currency]
  ): ValidatedNel[ParseCurrencyError, RatesProgramProtocol.GetRatesRequest] = {
    val from = handleCurrencyParseFailure(QueryParams.Names.FROM, maybeFrom)
    val to   = handleCurrencyParseFailure(QueryParams.Names.TO, maybeTo)
    (from, to).mapN(RatesProgramProtocol.GetRatesRequest)
  }

  private def handleCurrencyParseFailure(
      paramName: String,
      in: ValidatedNel[ParseFailure, Currency]
  ): ValidatedNel[ParseCurrencyError, Currency] =
    in.leftMap(_.map(parseFailure => ParseCurrencyError(paramName, parseFailure.sanitized)))

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}

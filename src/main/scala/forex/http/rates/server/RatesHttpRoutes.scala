package forex.http.rates.server

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.syntax.apply._
import cats.syntax.flatMap._
import forex.domain.Currency
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import forex.domain.Utils._
import forex.http.rates.server.models.QueryParams
import forex.http.rates.server.models.out.ParseCurrencyError
import forex.programs.rates.errors.Error
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ HttpRoutes, ParseFailure, Response }

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import forex.http.rates.server.models.Converters._
  import forex.http.rates.server.models.QueryParams._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      parseGetRatesRequest(from, to).fold(
        parsingErrors => BadRequest(parsingErrors),
        request => rates.get(request).flatMap(_.fold(handleServiceErrors, rate => Ok(rate.asGetApiResponse)))
      )
  }

  private def handleServiceErrors: PartialFunction[Error, F[Response[F]]] = {
    case Error.RateLookupUnreachable => BadGateway()
    // Like in our target One Frame Api we return empty list in that case
    case Error.MeaninglessRequest => Ok(List[Unit]())
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

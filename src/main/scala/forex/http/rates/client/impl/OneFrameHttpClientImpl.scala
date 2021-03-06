package forex.http.rates.client.impl

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.functor._
import forex.config.ClientConfig
import forex.domain.Utils._
import forex.http.rates.client.models.in._
import forex.http.rates.client.algebra.OneFrameHttpClient
import forex.http.rates.client.models.OneFrameHttpClientError._
import forex.http.rates.client.models._
import forex.http.rates.client.models.in.OneFrameResponse.{ ErrorJsonResponse, GetCurrenciesSuccessfulResponse }
import fs2.text
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{ Header, Headers, Method, Request, Response, Status, Uri }
import org.http4s.client.Client

import scala.util.control.NonFatal

/** HTTP client, which consumes the One Frame API.
  * handles situations when there is an empty response, unknown error, forbidden or some One Frame API error.
  */
final class OneFrameHttpClientImpl[F[_]: Sync](config: ClientConfig, client: Client[F]) extends OneFrameHttpClient[F] {
  private val targetUri = Uri.unsafeFromString(s"http://${config.targetHost}:${config.targetPort}/rates")
  private val headers   = Headers.of(Header("token", config.token))

  override def getCurrenciesRates(
      in: GetCurrenciesRequest
  ): F[OneFrameHttpClientError Either NonEmptyList[GetCurrencyValue]] = {
    val uri     = targetUri.withQueryParam("pair", in.currencies.toList)
    val request = Request[F](Method.GET, uri, headers = headers)
    doRequest(request).recover { case NonFatal(e) => ClientError(e).asLeft }
  }

  private def doRequest(request: Request[F]): F[OneFrameHttpClientError Either NonEmptyList[GetCurrencyValue]] =
    client
      .run(request)
      .use {
        case Status.Successful(resp) => parseSuccessfulResponseBody(resp)
        case Status.NotFound(_)      => NotFound.asLeft.pure[F].widen
        case Status.Forbidden(_)     => EndpointForbidden.asLeft.pure[F].widen
        case unknownResponse         => handleUnknownResponse(unknownResponse)
      }

  private def parseSuccessfulResponseBody(
      resp: Response[F]
  ): F[OneFrameHttpClientError Either NonEmptyList[GetCurrencyValue]] =
    resp
      .attemptAs[OneFrameResponse]
      .map {
        case GetCurrenciesSuccessfulResponse(in) =>
          NonEmptyList.fromList(in).toRight[OneFrameHttpClientError](EmptyResponse)
        case ErrorJsonResponse("Forbidden") => EndpointForbidden.asLeft
        case ErrorJsonResponse(error)       => ErrorResponse(error).asLeft
      }
      .foldF(_ => handleUnknownResponse(resp), _.pure[F])

  private def handleUnknownResponse[R](resp: Response[F]): F[OneFrameHttpClientError Either R] = {
    val body: F[String] = resp.body.through(text.utf8Decode).compile.string
    body.map(UnknownResponse(_).asLeft[R])
  }

}

object OneFrameHttpClientImpl {
  def apply[F[_]: Sync](config: ClientConfig, client: Client[F]): OneFrameHttpClientImpl[F] =
    new OneFrameHttpClientImpl(config, client)
}

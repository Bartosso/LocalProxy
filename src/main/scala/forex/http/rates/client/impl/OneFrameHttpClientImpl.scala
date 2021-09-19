package forex.http.rates.client.impl

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import forex.config.ClientConfig
import forex.http.rates.client.errors._
import forex.domain.Utils._
import forex.http.rates.client.Protocol.In.{
  ErrorJsonResponse,
  GetCurrenciesSuccessfulResponse,
  GetCurrencyValue,
  OneFrameResponse
}
import forex.http.rates.client.Protocol.Out.GetCurrenciesRequest
import forex.http.rates.client.algebra.OneFrameHttpClient
import fs2.text
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{ Header, Headers, Method, Request, Response, Status, Uri }
import org.http4s.client.Client

final class OneFrameHttpClientImpl[F[_]: Sync](config: ClientConfig, client: Client[F]) extends OneFrameHttpClient[F] {
  private val targetUri = Uri.unsafeFromString(s"http://${config.targetHost}:${config.targetPort}/rates")
  private val headers   = Headers.of(Header("token", config.token))

  override def getCurrenciesRates(
      in: GetCurrenciesRequest
  ): F[Either[OneFrameHttpClientError, NonEmptyList[GetCurrencyValue]]] = {
    val uri     = targetUri.withQueryParam("pair", in.currencies.toList)
    val request = Request[F](Method.GET, uri, headers = headers)
    doRequest(request)
  }

  private def doRequest(request: Request[F]): F[Either[OneFrameHttpClientError, NonEmptyList[GetCurrencyValue]]] =
    client.run(request).use {
      case Status.Successful(resp) => parseSuccessfulResponseBody(resp)
      case Status.NotFound(_)      => NotFound.asLeft.pure[F].widen
      case Status.Forbidden(_)     => EndpointForbidden.asLeft.pure[F].widen
      case unknownResponse         => handleUnknownResponse(unknownResponse)
    }

  private def parseSuccessfulResponseBody(
      resp: Response[F]
  ): F[Either[OneFrameHttpClientError, NonEmptyList[GetCurrencyValue]]] =
    resp
      .attemptAs[OneFrameResponse]
      .map {
        case GetCurrenciesSuccessfulResponse(in) =>
          NonEmptyList.fromList(in).toRight[OneFrameHttpClientError](EmptyResponse)
        case ErrorJsonResponse(error) => ErrorResponse(error).asLeft
      }
      .foldF(_ => handleUnknownResponse(resp), _.pure[F])

  private def handleUnknownResponse[R](resp: Response[F]): F[Either[OneFrameHttpClientError, R]] = {
    val body: F[String] = resp.body.through(text.utf8Decode).compile.string
    body.map(UnknownResponse(_).asLeft[R])
  }

}

object OneFrameHttpClientImpl {
  def apply[F[_]: Sync](config: ClientConfig, client: Client[F]): OneFrameHttpClientImpl[F] =
    new OneFrameHttpClientImpl(config, client)
}

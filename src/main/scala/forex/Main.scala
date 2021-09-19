package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import forex.config._
import forex.http.rates.client.OneFrameHttpClientImpl
import forex.services.rates.Interpreters
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import scalacache.Mode

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val mode: Mode[IO] = scalacache.CatsEffect.modes.async
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)
  }

}

class Application[F[_]: ConcurrentEffect: Timer: Mode] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      httpCli <- BlazeClientBuilder(ec).stream
      oneFrameCli = OneFrameHttpClientImpl(config.clientConfig, httpCli)
      oneFrameRestService <- Stream.resource(Interpreters.httpTarget(oneFrameCli, config.oneFrameConfig))
      module = new Module[F](config, oneFrameRestService)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}

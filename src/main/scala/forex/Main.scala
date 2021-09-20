package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import forex.config._
import forex.http.rates.client.impl.OneFrameHttpClientImpl
import forex.services.rates.Interpreters
import forex.services.rates.interpreters.CacheSynchronizationImpl
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import scalacache.Mode
import tofu.logging.Logs

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val mode: Mode[IO] = scalacache.CatsEffect.modes.async
    new Application[IO].resource(executionContext).use(_ => IO.never)
  }

}

class Application[F[_]: ConcurrentEffect: Timer: Mode: ContextShift] {

  def resource(ec: ExecutionContext): Resource[F, Unit] =
    for {
      blocker <- Blocker[F]
      config <- Config.resource("app", blocker)
      clientConfig = config.clientConfig
      httpCli <- BlazeClientBuilder(ec)
                  .withRequestTimeout(clientConfig.timeout)
                  .withIdleTimeout(clientConfig.idleTimeout)
                  .resource
      logs        = Logs.sync[F, F]
      oneFrameCli = OneFrameHttpClientImpl(config.clientConfig, httpCli)
      cache <- CacheSynchronizationImpl.createSyncedCache(oneFrameCli, config.oneFrameConfig, logs)
      oneFrameRestService <- Interpreters.cachedImpl(cache, logs)
      module = new Module[F](config, oneFrameRestService)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .resource
    } yield ()

}

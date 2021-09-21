package forex

import scala.concurrent.ExecutionContext
import cats.effect._
import forex.config._
import forex.http.rates.client.impl.OneFrameHttpClientImpl
import forex.services.rates.Interpreters
import forex.services.rates.interpreters.{ CacheImpl, CacheSynchronizationImpl }
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import scalacache.Mode
import tofu.logging.Logs

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val mode: Mode[IO] = scalacache.CatsEffect.modes.async
    val config                  = Blocker[IO].use(blocker => Config.load[IO]("app", blocker))
    Resource
      .eval(config)
      .flatMap(conf => new Application[IO].resource(executionContext, conf))
      .use(_ => IO.never)
  }

}

class Application[F[_]: ConcurrentEffect: Timer: Mode] {

  def resource(ec: ExecutionContext, config: ApplicationConfig): Resource[F, Unit] = {
    val clientConfig = config.clientConfig
    for {
      httpCli <- BlazeClientBuilder(ec)
                  .withRequestTimeout(clientConfig.timeout)
                  .withIdleTimeout(clientConfig.idleTimeout)
                  .resource
      logs        = Logs.sync[F, F]
      oneFrameCli = OneFrameHttpClientImpl(config.clientConfig, httpCli)
      cache <- CacheImpl.resource(config.cacheConfig, logs)
      _ <- CacheSynchronizationImpl.startCacheSynchronization(oneFrameCli, cache, config.cacheConfig, logs)
      oneFrameRestService <- Interpreters.cachedImpl(cache, logs)
      module = new Module[F](config, oneFrameRestService)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .resource
    } yield ()
  }

}

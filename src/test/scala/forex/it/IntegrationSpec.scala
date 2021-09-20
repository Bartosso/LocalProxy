package forex.it

import cats.effect.{ Blocker, ContextShift, IO, Timer }
import com.dimafeng.testcontainers.{ ForAllTestContainer, GenericContainer }
import forex.Application
import forex.http.rates.server.models.out.GetApiResponse
import forex.TestCodecs._
import forex.config.{ ApplicationConfig, Config }
import forex.domain.Utils.jsonDecoder
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.containers.wait.strategy.Wait
import scalacache.CatsEffect.modes.async

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.util.Try

class IntegrationSpec extends AnyWordSpec with Matchers with ForAllTestContainer {

  implicit lazy val context: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit lazy val timer: Timer[IO]          = IO.timer(ExecutionContext.global)

  lazy val config: ApplicationConfig = loadConfigAndSetTargetPort()
  val testUri                        = uri"http://localhost:8080/rates?from=JPY&to=USD"

  override val container: GenericContainer =
    GenericContainer("paidyinc/one-frame", exposedPorts = Seq(8080), waitStrategy = Wait.forHttp("/rates?pair=JPYUSD"))

  "Application" should {
    "work" in {
      val (_, releaseApp)         = new Application[IO].resource(ExecutionContext.global, config).allocated.unsafeRunSync()
      val (client, releaseClient) = BlazeClientBuilder[IO](global).resource.allocated.unsafeRunSync()
      val requestResult           = client.get(testUri)(response => response.as[GetApiResponse])
      val maybeGetApiResponse     = Try(requestResult.unsafeRunSync())
      maybeGetApiResponse.isSuccess shouldBe true
      releaseApp.unsafeRunSync()
      releaseClient.unsafeRunSync()
      succeed
    }
  }

  private def loadConfigAndSetTargetPort(): ApplicationConfig = {
    val config              = Blocker[IO].use(blocker => Config.load[IO]("app", blocker)).unsafeRunSync()
    val clientConfig        = config.clientConfig
    val updatedClientConfig = clientConfig.copy(targetPort = container.mappedPort(8080))
    config.copy(clientConfig = updatedClientConfig)
  }
}

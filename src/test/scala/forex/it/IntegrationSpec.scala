package forex.it

import cats.effect.{ Blocker, ContextShift, IO, Timer }
import com.dimafeng.testcontainers.{ ForAllTestContainer, GenericContainer }
import forex.Application
import forex.http.rates.server.models.out.{ GetApiResponse, ParseCurrencyError }
import forex.TestCodecs._
import forex.config.{ ApplicationConfig, Config }
import forex.domain.Utils.jsonDecoder
import forex.domain.Utils._
import org.http4s.FormDataDecoder.formEntityDecoder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.containers.wait.strategy.Wait
import scalacache.CatsEffect.modes.async

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.util.Try

class IntegrationSpec extends AnyWordSpec with Matchers with ForAllTestContainer with BeforeAndAfterAll {

  implicit lazy val context: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit lazy val timer: Timer[IO]          = IO.timer(ExecutionContext.global)

  lazy val config: ApplicationConfig = loadConfigAndSetTargetPort()
  lazy val (client, releaseClient)   = BlazeClientBuilder[IO](global).resource.allocated.unsafeRunSync()
  val testUri                        = uri"http://localhost:8080/rates?from=JPY&to=USD"

  override val container: GenericContainer =
    GenericContainer("paidyinc/one-frame", exposedPorts = Seq(8080), waitStrategy = Wait.forHttp("/rates?pair=JPYUSD"))

  override def afterAll(): Unit = releaseClient.unsafeRunSync()

  "Application" should {
    "work" in {
      val (_, releaseApp)     = new Application[IO].resource(ExecutionContext.global, config).allocated.unsafeRunSync()
      val requestResult       = client.get(testUri)(response => response.as[GetApiResponse])
      val maybeGetApiResponse = Try(requestResult.unsafeRunSync())
      maybeGetApiResponse.isSuccess shouldBe true
      releaseApp.unsafeRunSync()
      succeed
    }

    "return error if we pass invalid currency" in {
      val (_, releaseApp) = new Application[IO].resource(ExecutionContext.global, config).allocated.unsafeRunSync()
      val invalidUri      = uri"http://localhost:8080/rates?from=DSF&to=USD"
      val errorMessages = client
        .get(invalidUri) { response =>
          response.status.code shouldBe 400
          response.as[List[ParseCurrencyError]]
        }
        .unsafeRunSync()

      errorMessages shouldBe List(ParseCurrencyError("from", "unknown currency"))
      releaseApp.unsafeRunSync()
      succeed
    }

    "return empty response if `from` and `to` have the same currency" in {
      val (_, releaseApp) = new Application[IO].resource(ExecutionContext.global, config).allocated.unsafeRunSync()
      val invalidUri      = uri"http://localhost:8080/rates?from=USD&to=USD"
      val errorMessages = client
        .get(invalidUri) { response =>
          response.status.code shouldBe 200
          response.as[List[Unit]]
        }
        .unsafeRunSync()

      errorMessages shouldBe List()
      releaseApp.unsafeRunSync()
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

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
import org.http4s.{ Response, Uri }
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.{ Assertion, BeforeAndAfterAll }
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

  override val container: GenericContainer =
    GenericContainer("paidyinc/one-frame", exposedPorts = Seq(8080), waitStrategy = Wait.forHttp("/rates?pair=JPYUSD"))

  override def afterAll(): Unit = releaseClient.unsafeRunSync()

  "Application" should {
    "work" in {
      val testUri = uri"http://localhost:8080/rates?from=JPY&to=USD"
      withAppAndClient(testUri) { response =>
        response.status.code shouldBe 200
        val errorMessages = Try(response.as[GetApiResponse].unsafeRunSync())
        errorMessages.isSuccess shouldBe true
      }
    }

    "return error if we pass invalid currency" in {
      val invalidUri = uri"http://localhost:8080/rates?from=DSF&to=USD"
      withAppAndClient(invalidUri) { response =>
        response.status.code shouldBe 400
        val errorMessages = response.as[List[ParseCurrencyError]].unsafeRunSync()
        errorMessages shouldBe List(ParseCurrencyError("from", "unknown currency"))
      }
    }

    "return empty response if `from` and `to` have the same currency" in {
      val invalidUri = uri"http://localhost:8080/rates?from=USD&to=USD"
      withAppAndClient(invalidUri) { response =>
        response.status.code shouldBe 200
        val errorMessages = response.as[List[Unit]].unsafeRunSync()
        errorMessages shouldBe List()
      }
    }
  }

  private def loadConfigAndSetTargetPort(): ApplicationConfig = {
    val config              = Blocker[IO].use(blocker => Config.load[IO]("app", blocker)).unsafeRunSync()
    val clientConfig        = config.clientConfig
    val updatedClientConfig = clientConfig.copy(targetPort = container.mappedPort(8080))
    config.copy(clientConfig = updatedClientConfig)
  }

  private def withAppAndClient(uri: Uri)(assertFun: Response[IO] => Assertion): Unit = {
    val (_, releaseApp) = new Application[IO].resource(ExecutionContext.global, config).allocated.unsafeRunSync()
    client.get(uri)(response => IO(assertFun(response))).unsafeRunSync()
    releaseApp.unsafeRunSync()
  }
}

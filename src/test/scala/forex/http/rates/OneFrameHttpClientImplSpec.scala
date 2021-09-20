package forex.http.rates

import cats.data.NonEmptyList
import cats.effect.{ ContextShift, IO }
import cats.syntax.either._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, urlEqualTo }
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import forex.Generators
import forex.config.ClientConfig
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.domain.Rate.Pair
import forex.http.rates.client.impl.OneFrameHttpClientImpl
import forex.http.rates.client.models.OneFrameHttpClientError._
import forex.http.rates.client.models.in.GetCurrencyValue
import forex.http.rates.client.models.out.GetCurrencyValuePair
import forex.http.rates.client.models.{ GetCurrenciesRequest, OneFrameHttpClientError }
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt

class OneFrameHttpClientImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  val port     = 90
  val host     = "localhost"
  val hostName = s"$host:$port"

  val defaultRate: Rate = Generators.rate()
  val defaultPair: Pair = defaultRate.pair
  val defaultUri        = s"/rates?pair=${defaultPair.from.entryName}${defaultPair.to.entryName}"

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  lazy val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  private val dummyClientConfig = ClientConfig("10dc303535874aeccc86a8251e6992f5", host, port, 5.seconds, 5.seconds)

  override def beforeAll(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(host, port)
  }

  override def afterAll(): Unit = wireMockServer.stop()

  "OneFrameHttpClientImpl" should {
    "handle empty response (when `from` and `to` have the same value)" in {
      val uri               = "/rates?pair=USDUSD"
      val emptyResponseBody = "[]"
      stubWithBody(emptyResponseBody, uri, 200)
      val requestPair = GetCurrencyValuePair(Currency.USD, Currency.USD)

      val response = requestForValues(GetCurrenciesRequest(NonEmptyList.one(requestPair))).unsafeRunSync()
      response shouldBe EmptyResponse.asLeft
    }

    "handle response with a single value" in {
      val uri              = "/rates?pair=JPYUSD"
      val responseBodyStub = """[
                       |    {
                       |        "from": "JPY",
                       |        "to": "USD",
                       |        "bid": 0.5880130356779003,
                       |        "ask": 0.45219073323722314,
                       |        "price": 0.52010188445756172,
                       |        "time_stamp": "2021-09-20T09:30:10.343Z"
                       |    }
                       |]""".stripMargin
      stubWithBody(responseBodyStub, uri, 200)
      val requestPair = GetCurrencyValuePair(Currency.JPY, Currency.USD)
      val expectedValue = GetCurrencyValue(
        Currency.JPY,
        Currency.USD,
        BigDecimal("0.5880130356779003"),
        BigDecimal("0.45219073323722314"),
        Price(BigDecimal("0.52010188445756172")),
        Timestamp(OffsetDateTime.parse("2021-09-20T09:30:10.343Z"))
      )

      val response = requestForValues(GetCurrenciesRequest(NonEmptyList.one(requestPair))).unsafeRunSync()
      response shouldBe NonEmptyList.one(expectedValue).asRight
    }

    "handle response with severial values" in {
      val uri              = "/rates?pair=JPYUSD&pair=JPYCAD"
      val responseBodyStub = """[
                          |    {
                          |        "from": "JPY",
                          |        "to": "USD",
                          |        "bid": 0.3146093151604401,
                          |        "ask": 0.32557122573774677,
                          |        "price": 0.320090270449093435,
                          |        "time_stamp": "2021-09-20T10:12:34.59Z"
                          |    },
                          |    {
                          |        "from": "JPY",
                          |        "to": "CAD",
                          |        "bid": 0.5126621696889497,
                          |        "ask": 0.12119332902514046,
                          |        "price": 0.31692774935704508,
                          |        "time_stamp": "2021-09-20T10:12:34.59Z"
                          |    }
                          |]""".stripMargin
      stubWithBody(responseBodyStub, uri, 200)
      val requestPair1 = GetCurrencyValuePair(Currency.JPY, Currency.USD)
      val requestPair2 = GetCurrencyValuePair(Currency.JPY, Currency.CAD)
      val expectedValue1 = GetCurrencyValue(
        Currency.JPY,
        Currency.USD,
        BigDecimal("0.3146093151604401"),
        BigDecimal("0.32557122573774677"),
        Price(BigDecimal("0.320090270449093435")),
        Timestamp(OffsetDateTime.parse("2021-09-20T10:12:34.59Z"))
      )
      val expectedValue2 = GetCurrencyValue(
        Currency.JPY,
        Currency.CAD,
        BigDecimal("0.5126621696889497"),
        BigDecimal("0.12119332902514046"),
        Price(BigDecimal("0.31692774935704508")),
        Timestamp(OffsetDateTime.parse("2021-09-20T10:12:34.59Z"))
      )

      val response = requestForValues(GetCurrenciesRequest(NonEmptyList.of(requestPair1, requestPair2))).unsafeRunSync()
      response shouldBe NonEmptyList.of(expectedValue1, expectedValue2).asRight
    }

    //It's impossible
    "deal with response 200 but with error in body" in {
      val uri              = "/rates?pair=JPYUSD"
      val responseBodyStub = """{
                           |    "error": "Invalid Currency Pair"
                           |}""".stripMargin
      stubWithBody(responseBodyStub, uri, 200)
      val requestPair = GetCurrencyValuePair(Currency.JPY, Currency.USD)

      val response = requestForValues(GetCurrenciesRequest(NonEmptyList.one(requestPair))).unsafeRunSync()
      response shouldBe ErrorResponse("Invalid Currency Pair").asLeft
    }

    "deal with forbidden error with status 200" in {
      val uri              = "/rates?pair=JPYUSD"
      val responseBodyStub = """{
                           |    "error": "Forbidden"
                           |}""".stripMargin
      stubWithBody(responseBodyStub, uri, 200)
      val requestPair = GetCurrencyValuePair(Currency.JPY, Currency.USD)

      val response = requestForValues(GetCurrenciesRequest(NonEmptyList.one(requestPair))).unsafeRunSync()
      response shouldBe EndpointForbidden.asLeft
    }

    "deal with not found error if somehow endpoint disappeared" in {
      val uri              = "/rates?pair=JPYUSD"
      val responseBodyStub = ""
      stubWithBody(responseBodyStub, uri, 404)
      val requestPair = GetCurrencyValuePair(Currency.JPY, Currency.USD)

      val response = requestForValues(GetCurrenciesRequest(NonEmptyList.one(requestPair))).unsafeRunSync()
      response shouldBe NotFound.asLeft
    }

    "deal with unknown response (500)" in {
      val uri              = "/rates?pair=JPYUSD"
      val responseBodyStub = """{
                               |    "message": "oh no, server died"
                               |}""".stripMargin
      stubWithBody(responseBodyStub, uri, 500)
      val requestPair = GetCurrencyValuePair(Currency.JPY, Currency.USD)

      val response = requestForValues(GetCurrenciesRequest(NonEmptyList.one(requestPair))).unsafeRunSync()
      response shouldBe UnknownResponse("""{
                                          |    "message": "oh no, server died"
                                          |}""".stripMargin).asLeft
    }
  }

  def stubWithBody(body: String, uri: String, status: Int): StubMapping =
    wireMockServer.stubFor(
      WireMock
        .get(urlEqualTo(uri))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .withStatus(status)
        )
    )

  def requestForValues(
      request: GetCurrenciesRequest,
      config: ClientConfig = dummyClientConfig
  ): IO[Either[OneFrameHttpClientError, NonEmptyList[GetCurrencyValue]]] =
    BlazeClientBuilder[IO](global).resource.use { client =>
      OneFrameHttpClientImpl[IO](config, client).getCurrenciesRates(request)
    }
}

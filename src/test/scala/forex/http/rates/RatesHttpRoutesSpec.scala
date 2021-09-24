package forex.http.rates

import cats.effect.IO
import cats.syntax.either._
import forex.Generators
import forex.domain.Rate
import forex.http.rates.server.RatesHttpRoutes
import forex.programs.RatesProgram
import forex.domain.Utils._
import forex.http.rates.server.models.Converters.GetApiResponseOps
import forex.http.rates.server.models.out.{ GetApiResponse, ParseCurrencyError }
import forex.programs.rates.models.RatesRequestError
import forex.programs.rates.models.RatesRequestError.{ MeaninglessRequest, RateLookupUnreachable }
import forex.TestCodecs._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Request, Status, Uri }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RatesHttpRoutesSpec extends AnyWordSpec with Matchers {

  val dummyUri: Uri   = uri"/rates?from=JPY&to=USD"
  val dummyRate: Rate = Generators.rate()

  "RatesHttpRoutes" should {
    "return empty list request (as an original OneFrame api) when got MeaninglessRequest error from the service" in {
      val endpoints = new RatesHttpRoutes(dummyProgramWithGetResult(MeaninglessRequest.asLeft)).routes

      val maybeResult = endpoints.run(Request(uri = dummyUri)).value.unsafeRunSync()
      maybeResult.isDefined shouldBe true
      val Some(result) = maybeResult
      result.status shouldBe Status.Ok
      result.as[List[Unit]].unsafeRunSync() shouldBe List.empty[Unit]
    }

    "return bad gateway error when got RateLookupUnreachable error from the service" in {
      val endpoints = new RatesHttpRoutes(dummyProgramWithGetResult(RateLookupUnreachable.asLeft)).routes

      val maybeResult = endpoints.run(Request(uri = dummyUri)).value.unsafeRunSync()
      maybeResult.isDefined shouldBe true
      val Some(result) = maybeResult
      result.status shouldBe Status.BadGateway
      result.body.compile.toVector.unsafeRunSync().isEmpty shouldBe true
    }

    "return error if user passes unknown currencies" in {
      val unknownCurrenciesUri = uri"/rates?from=IDontKnow&to=OrMaybeIDo"
      val endpoints            = new RatesHttpRoutes(dummyProgramWithGetResult(dummyRate.asRight)).routes

      val maybeResult = endpoints.run(Request(uri = unknownCurrenciesUri)).value.unsafeRunSync()
      maybeResult.isDefined shouldBe true
      val Some(result) = maybeResult
      result.status shouldBe Status.BadRequest
      result.as[List[ParseCurrencyError]].unsafeRunSync() shouldBe List(
        ParseCurrencyError("from", "unknown currency"),
        ParseCurrencyError("to", "unknown currency")
      )
    }

    "return error if user passes wrong `from` currency" in {
      val unknownCurrenciesUri = uri"/rates?from=IDontKnow&to=USD"
      val endpoints            = new RatesHttpRoutes(dummyProgramWithGetResult(dummyRate.asRight)).routes

      val maybeResult = endpoints.run(Request(uri = unknownCurrenciesUri)).value.unsafeRunSync()
      maybeResult.isDefined shouldBe true
      val Some(result) = maybeResult
      result.status shouldBe Status.BadRequest
      result.as[List[ParseCurrencyError]].unsafeRunSync() shouldBe List(
        ParseCurrencyError("from", "unknown currency")
      )
    }

    "return error if user passes wrong `to` currency" in {
      val unknownCurrenciesUri = uri"/rates?from=USD&to=SomethingInvalid"
      val endpoints            = new RatesHttpRoutes(dummyProgramWithGetResult(dummyRate.asRight)).routes

      val maybeResult = endpoints.run(Request(uri = unknownCurrenciesUri)).value.unsafeRunSync()
      maybeResult.isDefined shouldBe true
      val Some(result) = maybeResult
      result.status shouldBe Status.BadRequest
      result.as[List[ParseCurrencyError]].unsafeRunSync() shouldBe List(ParseCurrencyError("to", "unknown currency"))
    }

    "return value when everything is ok" in {
      val unknownCurrenciesUri = uri"/rates?from=USD&to=EUR"
      val endpoints            = new RatesHttpRoutes(dummyProgramWithGetResult(dummyRate.asRight)).routes

      val maybeResult = endpoints.run(Request(uri = unknownCurrenciesUri)).value.unsafeRunSync()
      maybeResult.isDefined shouldBe true
      val Some(result) = maybeResult
      result.status shouldBe Status.Ok
      result.as[GetApiResponse].unsafeRunSync() shouldBe dummyRate.asGetApiResponse
    }
  }

  def dummyProgramWithGetResult(result: RatesRequestError Either Rate): RatesProgram[IO] = _ => IO.pure(result)

}

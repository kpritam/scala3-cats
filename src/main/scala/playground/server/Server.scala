package playground.server

import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import com.comcast.ip4s.*
import org.http4s.ember.server.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

given LoggerFactory[IO] = Slf4jFactory.create[IO]

val greetingService = HttpRoutes.of[IO]:
  case GET -> Root / "greet" / name => Ok(s"Hello, $name.")

object Server extends IOApp:

  private val tweets = Seq(
    Tweet(1, "Tweet 1"),
    Tweet(2, "Tweet 2"),
    Tweet(3, "Tweet 3"),
    Tweet(4, "Tweet 4")
  )

  override def run(args: List[String]): IO[ExitCode] =
    for
      tweetService <- TweetService.make(tweets)
      router <- IO.pure(
        Router(
          "/" -> greetingService,
          "/api" -> tweetRoutes(tweetService)
        ).orNotFound
      )
      server <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(router)
        .build
        .use(_ => IO.never)
    yield ExitCode.Success

package playground.server

import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import io.circe.generic.auto.*
import org.http4s.circe.*

case class Tweet(id: Int, message: String)
object Tweet:
  given EntityEncoder[IO, Tweet] = jsonEncoderOf[Tweet]
  given EntityEncoder[IO, Seq[Tweet]] = jsonEncoderOf[Seq[Tweet]]
  given EntityEncoder[IO, Option[Tweet]] = jsonEncoderOf[Option[Tweet]]

trait TweetService:
  def get(tweetId: Int): IO[Option[Tweet]] = ???
  def getPopular(): IO[Seq[Tweet]] = ???

class TweetServiceInMem(tweets: Ref[IO, Seq[Tweet]]) extends TweetService:
  override def get(tweetId: Int): IO[Option[Tweet]] =
    tweets.get.map(_.find(_.id == tweetId))

  override def getPopular(): IO[Seq[Tweet]] = tweets.get


def tweetRoutes(tweetService: TweetService) = HttpRoutes.of[IO]:
  case GET -> Root / "tweets" / "popular" => tweetService.getPopular().flatMap(Ok(_))
  case GET -> Root / "tweets" / IntVar(tweetId) =>
    tweetService.get(tweetId).flatMap(Ok(_))
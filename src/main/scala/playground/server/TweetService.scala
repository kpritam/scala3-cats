package playground.server

import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import io.circe.generic.auto.*
import org.http4s.circe.*

case class Tweet(id: Int, message: String)
object Tweet:
  given EntityEncoder[IO, Tweet] = jsonEncoderOf[Tweet]
  given EntityDecoder[IO, Tweet] = jsonOf[IO, Tweet]
  given EntityEncoder[IO, Seq[Tweet]] = jsonEncoderOf[Seq[Tweet]]
  given EntityEncoder[IO, Option[Tweet]] = jsonEncoderOf[Option[Tweet]]

trait TweetService:
  def insert(tweet: Tweet): IO[Unit]
  def get(tweetId: Int): IO[Option[Tweet]]
  def getPopular(): IO[Seq[Tweet]]

object TweetService:
  def make(tweets: Seq[Tweet]): IO[TweetService] =
    Ref.of[IO, Seq[Tweet]](tweets).map(ref => new TweetServiceInMem(ref))

class TweetServiceInMem(tweets: Ref[IO, Seq[Tweet]]) extends TweetService:
  override def insert(tweet: Tweet): IO[Unit] =
    tweets.update(tweet +: _)

  override def get(tweetId: Int): IO[Option[Tweet]] =
    tweets.get.map(_.find(_.id == tweetId))

  override def getPopular(): IO[Seq[Tweet]] = tweets.get

def tweetRoutes(tweetService: TweetService) = HttpRoutes.of[IO]:
  case GET -> Root / "tweets" / "popular" =>
    tweetService.getPopular().flatMap(Ok(_))
  case GET -> Root / "tweets" / IntVar(tweetId) =>
    tweetService.get(tweetId).flatMap(Ok(_))
  case req @ POST -> Root / "tweets" =>
    for
      tweet <- req.as[Tweet]
      _ <- tweetService.insert(tweet)
      resp <- Ok(s"Tweet inserted: ${tweet.message}")
    yield resp

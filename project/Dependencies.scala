import sbt._

object Dependencies {
  lazy val munit = "org.scalameta" %% "munit" % "0.7.29"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.4"
  lazy val kittens = "org.typelevel" %% "kittens" % "3.3.0"

  lazy val fs2 = "co.fs2" %% "fs2-core" % "3.10.2"
  
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.13"
}

object Http4s {
  val version = "1.0.0-M41"

  val emberClient = "org.http4s" %% "http4s-ember-client" % version
  val emberServer = "org.http4s" %% "http4s-ember-server" % version
  val dsl = "org.http4s" %% "http4s-dsl" % version

  val all = Seq(emberClient, emberServer, dsl)
}

object Log4Cats {
  private val version = "2.7.0"

  val core = "org.typelevel" %% "log4cats-core" % version
  val slf4j = "org.typelevel" %% "log4cats-slf4j" % version

  val all = Seq(core, slf4j)
}

object Circe {
  val version = "0.14.6"

  val core = "io.circe" %% "circe-core" % version
  val http4sCirce = "org.http4s" %% "http4s-circe" % Http4s.version
  val generic = "io.circe" %% "circe-generic" % version
  val literal = "io.circe" %% "circe-literal" % version

  val all = Seq(core, http4sCirce, generic, literal)
}

object Akka {
  val version = "2.9.3"
  val actor = "com.typesafe.akka" %% "akka-actor-typed" % version

  val all = Seq(actor)
}

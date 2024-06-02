import Dependencies._

ThisBuild / scalaVersion := "3.5.0-RC1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "playground"
ThisBuild / Compile / run / fork := true

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

lazy val root = (project in file("."))
  .settings(
    name := "Scala Playground",
    libraryDependencies ++= List(
      catsEffect,
      kittens,
      fs2,
      logback,
      munit % Test
    ) ++
      Http4s.all ++
      Log4Cats.all ++
      Circe.all ++
      Akka.all,
    dependencyOverrides += "org.slf4j" % "slf4j-api" % "1.7.36",
    scalacOptions ++= List(
      "-Wnonunit-statement",
      "-new-syntax",
      "-rewrite",
      "-Wunused:all"
    )
  )

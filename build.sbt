import Dependencies._

ThisBuild / scalaVersion := "3.4.2"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "playground"
ThisBuild / Compile / run / fork := true

lazy val root = (project in file("."))
  .settings(
    name := "Scala Playground",
    libraryDependencies ++= List(
      catsEffect,
      kittens,
      fs2,
      munit % Test
    ) ++ 
    Http4s.all ++ 
    Log4Cats.all ++
    Circe.all,
    scalacOptions ++= List(
      "-Wnonunit-statement",
      "-new-syntax",
      "-rewrite",
      "-Wunused:all"
    )
  )

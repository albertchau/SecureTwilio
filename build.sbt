name := "SecureTwilio"

version := "1.0"

lazy val `securetwilio` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq( jdbc , anorm , cache , ws,
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.typesafe.play" %% "play-slick" % "0.8.1",
  "com.github.tototoshi" %% "slick-joda-mapper" % "1.2.0")

libraryDependencies += "ws.securesocial" %% "securesocial" % "master-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  
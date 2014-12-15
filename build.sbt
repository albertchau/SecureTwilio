name := "SecureTwilio"

version := "1.0"

lazy val `securetwilio` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq( jdbc , anorm , cache , ws )

libraryDependencies += "ws.securesocial" %% "securesocial" % "master-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  
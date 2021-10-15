name := "sdl"

version := "0.1"

scalaVersion := "3.0.2"

libraryDependencies += "org.scodec" %% "scodec-core" % "2.0.0"
libraryDependencies += "org.scodec" %% "scodec-bits" % "1.1.29"
libraryDependencies += "org.scodec" %% "scodec-stream" % "3.0.2"
libraryDependencies += "co.fs2" %% "fs2-io" % "3.1.5"
libraryDependencies += ("org.scalanlp" %% "breeze" % "2.0").exclude("org.typelevel", "cats-kernel_2.13")
libraryDependencies += "org.typelevel" %% "cats-kernel" % "2.6.1"
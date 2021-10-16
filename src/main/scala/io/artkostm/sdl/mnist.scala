package io.artkostm.sdl

import cats.effect.*
import cats.implicits.*

import fs2.io.file.*

import io.artkostm.sdl.mnist.*


object MnistExampleApp extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    MnistStreams[IO]("/Users/artkostm/Downloads/train-labels-idx1-ubyte 2", "/Users/artkostm/Downloads/train-images-idx3-ubyte 2")
      .take(3)
      .evalMap((label, pixels) => pixels.print[IO])
      .compile
      .fold(0)((acc, _) => acc + 1)
      .map(println)
      .as(ExitCode.Success)


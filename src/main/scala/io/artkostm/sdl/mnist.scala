package io.artkostm.sdl

import scodec.*
import scodec.bits.*
import scodec.codecs.*
import scodec.stream.*
import cats.*
import cats.effect.*
import cats.implicits.*

import java.nio.ByteBuffer
import java.nio.file.Paths

import breeze.linalg.operators.HasOps.*
import breeze.linalg.DenseMatrix

import fs2.io.file.*


final case class MnistImageDSMetadata(numberOfImages: Long, width: Long, height: Long) derives Codec

object MnistCodecs:
  val imageMetadataCodec =
    (("control_value" | constant(hex"00000803".bits)) ~>
      ("number_of_images" | uint32) ::
      ("width" | uint32) ::
      ("height" | uint32)).as[MnistImageDSMetadata]

  val labelMetadataCodec =
    ("control_value" | constant(hex"00000801".bits)) ~>
      ("number_of_labels" | uint32)

object MnistStreams:
  private val BufferSize: Int = 4096

  private val imageDecoder: StreamDecoder[DenseMatrix[Double]] =
    for
      metadata      <- StreamDecoder.once(MnistCodecs.imageMetadataCodec)
      bytesPerImage = metadata.width * metadata.height
      imageBytes    <- StreamDecoder.many(bytes(bytesPerImage.toInt))
    yield DenseMatrix.create[Double](
      metadata.height.toInt,
      metadata.width.toInt,
      imageBytes.toArray.map(b => (b & 0xFF).toDouble / 255.0)
    )

  private val labelsDecoder: StreamDecoder[Int] =
    for
      numberOfLabels <- StreamDecoder.once(MnistCodecs.labelMetadataCodec)
      label          <- StreamDecoder.many(uint8)
    yield label

  private def streamData[F[_] : Files : Concurrent, A](path: Path, decoder: StreamDecoder[A]): fs2.Stream[F, A] =
    Files[F].readAll(path, BufferSize, Flags(List(Flag.Read)))
      .chunks
      .map(c => BitVector.view(c.toArray))
      .through(decoder.toPipe)

  def labels(path: String): fs2.Stream[IO, Int] = streamData[IO, Int](Path(path), labelsDecoder)

  def images(path: String): fs2.Stream[IO, DenseMatrix[Double]] =
    streamData[IO, DenseMatrix[Double]](Path(path), imageDecoder).map(_.t)


object MnistApp extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    MnistStreams.labels("/Users/artkostm/Downloads/train-labels-idx1-ubyte 2")
      .zip(MnistStreams.images("/Users/artkostm/Downloads/train-images-idx3-ubyte 2"))
      .take(10)
      .map((label, pixels) =>
        println(label)
        printToConsole(pixels)
      )
      .compile
      .fold(0)((acc, _) => acc + 1)
      .map(println)
      .as(ExitCode.Success)

  def printToConsole(pixels: DenseMatrix[Double]): Unit =
    (0 until pixels.rows).foreach { row =>
      (0 until pixels.cols).foreach(column => if pixels(row, column) > 0.1 then print("*") else print(" "))
      println()
    }

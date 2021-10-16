package io.artkostm.sdl

import breeze.linalg.{DenseMatrix, DenseVector}
import fs2.io.file.*
import scodec.*
import scodec.bits.*
import scodec.codecs.*
import scodec.stream.*
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.data.*
import cats.effect.std.Console

package object mnist:
  opaque type Digit = Int
  object Digit:
    def apply(i: Int): Digit = i

  extension (d: Digit)
    def toVector: DenseVector[Double] =
      val labelVector = DenseVector.zeros[Double](10)
      labelVector(d) = 1.0
      labelVector

  opaque type Image = DenseMatrix[Double]
  object Image:
    def apply(matrix: DenseMatrix[Double]): Image = matrix

  extension (img: Image)
    def print[F[_]: Applicative](using C: Console[F]): F[Unit] =
      (0 until img.rows).map { row =>
        (0 until img.cols).map(column => if img(row, column) > 0.1 then C.print("*") else C.print(" ")) :+ C.println("")
      }
        .flatMap(identity)
        .toList
        .sequence
        .void

  final case class MnistImageDSMetadata(numberOfImages: Long, width: Long, height: Long) derives Codec

  object MnistCodecs:
    val labelMetadataCodec =
      ("control_value" | constant(hex"00000801".bits)) ~>
        ("number_of_labels" | uint32)

    val imageMetadataCodec =
      (("control_value" | constant(hex"00000803".bits)) ~>
        ("number_of_images" | uint32) ::
        ("width" | uint32) ::
        ("height" | uint32)).as[MnistImageDSMetadata]

  object MnistStreams:
    private val BufferSize: Int = 4096

    private val imageDecoder: StreamDecoder[Image] =
      for
        metadata      <- StreamDecoder.once(MnistCodecs.imageMetadataCodec)
        bytesPerImage = metadata.width * metadata.height
        imageBytes    <- StreamDecoder.many(bytes(bytesPerImage.toInt))
      yield Image(DenseMatrix.create[Double](
        metadata.height.toInt,
        metadata.width.toInt,
        imageBytes.toArray.map(b => (b & 0xFF).toDouble / 255.0)
      ))

    private val labelsDecoder: StreamDecoder[Digit] =
      for
        numberOfLabels <- StreamDecoder.once(MnistCodecs.labelMetadataCodec)
        label          <- StreamDecoder.many(uint8)
      yield Digit(label)

    private def streamData[F[_] : Files : Concurrent, A](path: Path, decoder: StreamDecoder[A]): fs2.Stream[F, A] =
      Files[F].readAll(path, BufferSize, Flags(List(Flag.Read)))
        .chunks
        .map(c => BitVector.view(c.toArray))
        .through(decoder.toPipe)

    def apply[F[_] : Files : Concurrent](labelsPath: String, imagesPath: String): fs2.Stream[F, (Digit, Image)] =
      streamData[F, Int](Path(labelsPath), labelsDecoder)
        .zip(streamData[F, Image](Path(imagesPath), imageDecoder).map(_.t))


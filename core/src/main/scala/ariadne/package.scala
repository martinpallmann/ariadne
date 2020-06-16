/*
 * Copyright (c) 2019 by Rob Norris
 * Copyright (c) 2020 by Martin Pallmann
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import zio._

package object ariadne {

  type EntryPoint = Has[EntryPoint.Service]

  object EntryPoint {

    /**
      * An entry point, for creating root spans or continuing traces that were started on another
      * system.
      */
    trait Service {

      /** Resource that creates a new root span in a new trace. */
      def root(name: String): UManaged[Span.Service] =
        cont(name, Kernel(Map.empty))

      /**
        * Resource that attempts to creates a new span as with `continue`, but falls back to a new root
        * span as with `root` if the kernel does not contain the required headers. In other words, we
        * continue the existing span if we can, otherwise we start a new one.
        */
      def cont(name: String, kernel: Kernel): UManaged[Span.Service]
    }

    def root(name: String): ZLayer[EntryPoint, Nothing, Span] =
      ZLayer.fromFunctionManaged[EntryPoint, Nothing, Span.Service](
        _.get.root(name)
      )

    def cont(name: String, kernel: Kernel): ZLayer[EntryPoint, Nothing, Span] =
      ZLayer.fromFunctionManaged[EntryPoint, Nothing, Span.Service](
        _.get.cont(name, kernel)
      )
  }

  type Span = Has[Span.Service]

  /** An span that can be passed around and used to create child spans. */
  object Span {
    trait Service {

      /** Put a sequence of fields into this span. */
      def put(fields: (String, TraceValue)*): URIO[Span.Service, Unit]

      /**
        * The kernel for this span, which can be sent as headers to remote systems, which can then
        * continue this trace by constructing spans that are children of this one.
        */
      def kernel: URIO[Span.Service, Kernel]

      /** Resource that yields a child span with the given name. */
      def span(name: String): UManaged[Span.Service]
    }

    def put(fields: (String, TraceValue)*): ZIO[Span, Nothing, Unit] =
      ZIO.accessM[Span](x => x.get.put(fields: _*).provide(x.get))

    def kernel: ZIO[Span, Nothing, Kernel] =
      ZIO.accessM[Span](x => x.get.kernel.provide(x.get))

    def span[R, E, A](name: String)(
      z: ZIO[Span with Has[R], E, A]
    )(implicit tr: Tag[R]): ZIO[Span with Has[R], E, A] =
      ZIO.accessM[Span with Has[R]](
        x => x.get.span(name).use(s => z.provide(x ++ Has(s)))
      )
  }

  /**
    * Mixin trait for exceptions that provide trace data. This allows exception data to be recorded
    * for spans that fail.
    */
  trait Fields {
    def fields: Map[String, TraceValue]
  }

  /**
    * An opaque hunk of data that can we can hand off to another system (in the form of HTTP headers),
    * which can then create new spans as children of this one. By this mechanism we allow our trace
    * to span remote calls.
    */
  final case class Kernel(toHeaders: Map[String, String])

  sealed trait TraceValue extends Product with Serializable {
    def value: Any
  }

  object TraceValue {

    case class StringValue(value: String) extends TraceValue
    case class BooleanValue(value: Boolean) extends TraceValue
    case class NumberValue(value: Number) extends TraceValue

    implicit def stringToTraceValue(value: String): TraceValue =
      StringValue(value)
    implicit def boolToTraceValue(value: Boolean): TraceValue =
      BooleanValue(value)
    implicit def intToTraceValue(value: Int): TraceValue =
      NumberValue(value)
  }
}

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
      def root(name: String): UManaged[Span] = cont(name, Kernel(Map.empty))

      /**
        * Resource that attempts to creates a new span as with `continue`, but falls back to a new root
        * span as with `root` if the kernel does not contain the required headers. In other words, we
        * continue the existing span if we can, otherwise we start a new one.
        */
      def cont(name: String, kernel: Kernel): UManaged[Span]
    }

    def root[E, A](name: String)(k: ZIO[Span, E, A]): ZIO[Has[Service], E, A] =
      cont(name, Kernel(Map.empty))(k)

    def cont[E, A](name: String, kernel: Kernel)(
      k: ZIO[Span, E, A]
    ): ZIO[Has[Service], E, A] =
      ZIO.accessM[Has[Service]](
        _.get[Service]
          .cont(name, kernel)
          .use(k.provide)
      )
  }

  /** An span that can be passed around and used to create child spans. */
  trait Span {

    /** Put a sequence of fields into this span. */
    def put(fields: (String, TraceValue)*): URIO[Span, Unit]

    /**
      * The kernel for this span, which can be sent as headers to remote systems, which can then
      * continue this trace by constructing spans that are children of this one.
      */
    def kernel: URIO[Span, Kernel]

    /** Resource that yields a child span with the given name. */
    def span(name: String): UManaged[Span]
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

  trait Trace {

    /** Put a sequence of fields into the current span. */
    def put(fields: (String, TraceValue)*): URIO[Span, Unit]

    /**
      * The kernel for the current span, which can be sent as headers to remote systems, which can
      * then continue this trace by constructing spans that are children of the current one.
      */
    def kernel: ZIO[Span, Nothing, Kernel]

    /** Create a new span, and within it run the continuation `k`. */
    def span[R, E, A](name: String)(k: ZIO[Span, E, A]): ZIO[Span, E, A]
  }

  object Trace {

    def apply()(implicit ev: Trace): ev.type = ev

    implicit def instance: Trace = new Trace {
      def put(fields: (String, TraceValue)*): URIO[Span, Unit] =
        ZIO.accessM[Span](_.put(fields: _*))

      def kernel: URIO[Span, Kernel] =
        ZIO.accessM[Span](_.kernel)

      def span[R, E, A](name: String)(k: ZIO[Span, E, A]): ZIO[Span, E, A] =
        ZIO.accessM[Span](
          _.span(name).use(
            s =>
              k.provideSome[Span](
                _ =>
                  new Span {
                    def put(fields: (String, TraceValue)*): URIO[Span, Unit] =
                      s.put(fields: _*)
                    def kernel: ZIO[Span, Nothing, Kernel] = s.kernel
                    def span(name: String): UManaged[Span] = s.span(name)
                }
            )
          )
        )
    }
  }

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

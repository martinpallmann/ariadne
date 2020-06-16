/*
 * Copyright (c) 2019 by Rob Norris
 * Copyright (c) 2020 by Martin Pallmann
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ariadne.opentracing

import ariadne.TraceValue.{BooleanValue, NumberValue, StringValue}
import io.{opentracing => ot}
import io.opentracing.propagation.TextMapAdapter
import io.opentracing.propagation.Format.Builtin.HTTP_HEADERS
import ariadne.{Kernel, Span, TraceValue}
import zio.{UIO, UManaged, ZIO, ZManaged}

import scala.jdk.CollectionConverters._

private[ariadne] final case class OpentracingSpan(tracer: ot.Tracer,
                                                  span: ot.Span)
    extends Span.Service {

  def put(fields: (String, TraceValue)*): UIO[Unit] =
    ZIO
      .foreach(fields) {
        case (k, StringValue(v))  => ZIO.succeed(span.setTag(k, v))
        case (k, NumberValue(v))  => ZIO.succeed(span.setTag(k, v))
        case (k, BooleanValue(v)) => ZIO.succeed(span.setTag(k, v))
      }
      .as(())

  def kernel: UIO[Kernel] =
    ZIO.succeed {
      val m = new java.util.HashMap[String, String]
      tracer.inject(span.context, HTTP_HEADERS, new TextMapAdapter(m))
      Kernel(m.asScala.toMap)
    }

  def span(name: String): UManaged[Span.Service] =
    ZManaged
      .make(ZIO.succeed(tracer.buildSpan(name).asChildOf(span).start()))(
        s => ZIO.succeed(s.finish())
      )
      .map(OpentracingSpan(tracer, _))
}

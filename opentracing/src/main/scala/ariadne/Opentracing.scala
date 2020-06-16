/*
 * Copyright (c) 2019 by Rob Norris
 * Copyright (c) 2020 by Martin Pallmann
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ariadne

import ariadne.opentracing.OpentracingSpan
import io.{opentracing => ot}
import io.opentracing.propagation.TextMapAdapter
import io.opentracing.propagation.Format.Builtin.HTTP_HEADERS
import zio._

import scala.jdk.CollectionConverters._

object Opentracing {

  def apply[R](tracer: R => ot.Tracer): ZLayer[R, Nothing, EntryPoint] =
    ZLayer.fromManaged(
      Managed
        .make(URIO.fromFunction(tracer))(t => ZIO.succeed(t.close()))
        .map(new OpentracingService(_))
    )

  private final class OpentracingService(t: ot.Tracer)
      extends EntryPoint.Service {

    private def extractCtxFrom(kernel: Kernel) =
      t.extract(HTTP_HEADERS, new TextMapAdapter(kernel.toHeaders.asJava))

    private def finish(s: ot.Span): ZIO[Any, Nothing, Unit] =
      ZIO.succeed(s.finish())

    def cont(name: String,
             kernel: Kernel): ZManaged[Any, Nothing, Span.Service] =
      Managed
        .make {
          ZIO(extractCtxFrom(kernel))
            .catchAll(_ => ZIO.succeed(null)) // TODO: well, this is probably not nice. Maybe add a debug log?
            .map(ctx => t.buildSpan(name).asChildOf(ctx).start())
        } { finish }
        .map(OpentracingSpan(t, _))
  }
}

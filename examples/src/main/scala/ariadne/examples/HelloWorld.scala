/*
 * Copyright (c) 2019 by Rob Norris
 * Copyright (c) 2020 by Martin Pallmann
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ariadne.examples

import java.io.IOException

import ariadne.{EntryPoint, Kernel, Opentracing, Span, Trace}
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.{Scope, ScopeManager, SpanContext, Tracer, tag}
import io.opentracing.mock.{MockSpan, MockTracer}
import io.opentracing.propagation.Format
import zio._
import zio.console._

object HelloWorld extends App {

  var tracings: List[MockSpan] = List.empty

  private val tracer: Any => Tracer = _ =>
    new MockTracer() {
      override def onSpanFinished(mockSpan: MockSpan): Unit =
        tracings = mockSpan :: tracings
  }

  def print(ts: List[MockSpan]): String =
    ts.sortBy(x => (x.parentId(), x.context().spanId())).mkString("\n")

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    logic
      .provideCustomLayer(root("root"))
      .exitCode
      .tap(_ => putStrLn(print(tracings)))

  val logic: ZIO[Span with Console, IOException, Unit] = for {
    _ <- Span.span[Console.Service, Nothing, Unit]("question") {
      Span.span[Console.Service, Nothing, Unit]("nested") {
        putStrLn("Hi, what's your name?")
      }
    }
    x <- Span.span("answer") { getStrLn }
    _ <- Span.span("response") { putStrLn(s"Hello, $x") }
  } yield ()

  def root(name: String): ZLayer[Any, Nothing, Span] =
    Opentracing(tracer) >>> EntryPoint.root(name)

}

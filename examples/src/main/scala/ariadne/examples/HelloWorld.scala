/*
 * Copyright (c) 2019 by Rob Norris
 * Copyright (c) 2020 by Martin Pallmann
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ariadne.examples

import java.io.IOException

import ariadne.{EntryPoint, Kernel, Opentracing, Trace}
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.{Scope, ScopeManager, Span, SpanContext, Tracer, tag}
import io.opentracing.mock.{MockSpan, MockTracer}
import io.opentracing.propagation.Format
import zio._
import zio.console._

object HelloWorld extends App {

  private val tracer: Any => Tracer = _ =>
    new MockTracer() {
      override def onSpanFinished(mockSpan: MockSpan): Unit =
        println(mockSpan)
  }

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    myAppLogic
      .provideCustomLayer(Opentracing(tracer))
      .exitCode

  val myAppLogic: ZIO[Console with EntryPoint, Nothing, Unit] =
    EntryPoint
      .cont("root", Kernel(Map("spanid" -> "991", "traceid" -> "992"))) {
        Trace().span("app_logic")(for {
          _ <- Trace().span("put_str_1")(putStr("Hello! What is your name?"))
          b <- Trace().kernel
        } yield System.out.println(b))
      }
//      .mapError(_ => new IOException())
//        .use(
//          span =>
//            for {
//              _ <- putStrLn("Hello! What is your name?")
//              name <- getStrLn
//              _ <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
//            } yield ()
//        )

}

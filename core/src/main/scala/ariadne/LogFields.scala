/*
 * Copyright (c) 2019 by Rob Norris
 * Copyright (c) 2020 by Martin Pallmann
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ariadne

object LogFields {

  /**
    * A stable identifier for some notable moment in the lifetime of a Span.
    * For instance, a mutex lock acquisition or release or the sorts of lifetime
    * events in a browser page load described in the Performance.timing specification.
    * E.g., from Zipkin, "cs", "sr", "ss", or "cr".
    * Or, more generally, "initialized" or "timed out". For errors, "error"
    */
  def event(e: String): (String, Any) = ("event", e)

  object event {
    def error: (String, Any) = event("error")
  }

  /**
    * A concise, human-readable, one-line message explaining the event.
    * E.g., "Could not connect to backend", "Cache invalidation succeeded"
    */
  def message(m: String): (String, Any) = ("message", m)

  /**
    * A stack trace in platform-conventional format; may or may not pertain to an error.
    * E.g., "File \"example.py\", line 7, in \<module\>\ncaller()\nFile \"example.py\",
    * line 5, in caller\ncallee()\nFile \"example.py\",
    * line 2, in callee\nraise Exception(\"Yikes\")\n"
    */
  def stack(s: String): (String, Any) = ("stack", s)

  object error {

    private val prefix = "error"

    /**
      * The type or "kind" of an error (only for event="error" logs).
      * E.g., "Exception", "OSError"
      */
    def kind(k: String): (String, Any) = (s"$prefix.kind", k)

    object kind {
      def exception: (String, Any) = kind("Exception")
      def osError: (String, Any) = kind("OsError")
    }

    /**
      * the actual Throwable/Exception/Error object instance itself. E.g.,
      * A java.lang.UnsupportedOperationException instance
      */
    def obj(o: Any): (String, Any) = (s"$prefix.object", o)
  }
}

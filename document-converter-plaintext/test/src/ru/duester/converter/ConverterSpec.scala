package ru.duester.converter

import zio.Scope
import zio.test.*
import zio.test.ZIOSpecDefault
import zio.test.test

object ConverterSpec extends ZIOSpecDefault:
  def spec: Spec[TestEnvironment, Any] =
    suite("test converter")(
      test("success"):
        assertTrue(true)
    )

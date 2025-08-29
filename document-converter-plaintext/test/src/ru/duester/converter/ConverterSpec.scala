package ru.duester.converter

import ru.duester.converter.model.IntermediateDocument
import ru.duester.converter.model.IntermediateNode
import ru.duester.plaintext.model.Paragraph
import ru.duester.plaintext.model.PlainTextDocument
import zio.Scope
import zio.test.*
import zio.test.Assertion.*
import zio.test.ZIOSpecDefault
import zio.test.test

object ConverterSpec extends ZIOSpecDefault:
  def spec: Spec[TestEnvironment, Any] =
    import ru.duester.converter.converter.*
    import ru.duester.converter.Converter.given
    suite("ConverterSpec")(
      test("toIntermediate_success_empty"):
        val srcDocument =
          PlainTextDocument(metadata = Map("title" -> "test document"))
        val document = srcDocument.toIntermediate
        assert(document)(
          hasField("nodes", (d: IntermediateDocument) => d.nodes, isEmpty)
            && hasField(
              "metadata",
              (d: IntermediateDocument) => d.metadata,
              hasKey("title", equalTo("test document"))
            )
        )
      ,
      test("toIntermediate_success_notEmpty"):
        val srcDocument =
          PlainTextDocument(
            List(Paragraph("header"), Paragraph("footer")),
            Map("title" -> "test document")
          )
        val document = srcDocument.toIntermediate
        assert(document)(
          hasField(
            "nodes",
            (d: IntermediateDocument) => d.nodes,
            hasSize(equalTo(2))
          )
            && hasField(
              "nodeType",
              (d: IntermediateDocument) => d.nodes.head.nodeType,
              equalTo("text")
            )
            && hasField(
              "attributes",
              (d: IntermediateDocument) => d.nodes.head.attributes,
              hasKey("data", equalTo("header"))
            )
            && hasField(
              "nodeType",
              (d: IntermediateDocument) => d.nodes.tail.head.nodeType,
              equalTo("text")
            )
            && hasField(
              "attributes",
              (d: IntermediateDocument) => d.nodes.tail.head.attributes,
              hasKey("data", equalTo("footer"))
            )
            && hasField(
              "metadata",
              (d: IntermediateDocument) => d.metadata,
              hasKey("title", equalTo("test document"))
            )
        )
      ,
      test("to_success_empty"):
        val document =
          IntermediateDocument(metadata = Map("title" -> "test document"))
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            isEmpty
          )
            && hasField(
              "metadata",
              (d: PlainTextDocument) => d.metadata,
              hasKey("title", equalTo("test document"))
            )
        )
      ,
      test("to_success_flat_notEmpty"):
        val document =
          IntermediateDocument(
            List(
              IntermediateNode("text", attributes = Map("data" -> "header")),
              IntermediateNode(
                "other",
                attributes = Map("data" -> "some text")
              ),
              IntermediateNode("text", attributes = Map("key" -> "value")),
              IntermediateNode("text", attributes = Map("data" -> "footer")),
              IntermediateNode("other", attributes = Map("key" -> "value"))
            ),
            Map("title" -> "test document")
          )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            hasSize(equalTo(2))
          )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.head.text,
              equalTo("header")
            )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.tail.head.text,
              equalTo("footer")
            )
            && hasField(
              "metadata",
              (d: PlainTextDocument) => d.metadata,
              hasKey("title", equalTo("test document"))
            )
        )
      ,
      test("to_success_nested"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "other",
              List(IntermediateNode("text", attributes = Map("data" -> "text")))
            ),
            IntermediateNode(
              "another",
              List(
                IntermediateNode(
                  "yet another",
                  List(
                    IntermediateNode(
                      "text",
                      attributes = Map("data" -> "deeply nested")
                    )
                  )
                )
              )
            )
          )
        )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            hasSize(equalTo(2))
          )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.head.text,
              equalTo("text")
            )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.tail.head.text,
              equalTo("deeply nested")
            )
        )
    )

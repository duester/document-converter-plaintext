package ru.duester.plaintext.converter

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
    import ru.duester.plaintext.converter.Converter.given

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
      test("to_success_text_flat_notEmpty"):
        val document =
          IntermediateDocument(
            List(
              IntermediateNode("text", attributes = Map("data" -> "header")),
              IntermediateNode(
                "other",
                attributes = Map("data" -> "some text")
              ),
              IntermediateNode("text", attributes = Map("key" -> "value")),
              IntermediateNode("text"),
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
            hasSize(equalTo(3))
          )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.head.text,
              equalTo("header")
            )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.tail.head.text,
              equalTo("\n")
            )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.tail.tail.head.text,
              equalTo("footer")
            )
            && hasField(
              "metadata",
              (d: PlainTextDocument) => d.metadata,
              hasKey("title", equalTo("test document"))
            )
        )
      ,
      test("to_success_text_nested"):
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
      ,
      test("to_success_heading"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "heading",
              List(
                IntermediateNode(
                  "heading.title",
                  List(
                    IntermediateNode("text", Nil, Map("data" -> "Text with ")),
                    IntermediateNode(
                      "emphasis",
                      List(
                        IntermediateNode(
                          "text",
                          Nil,
                          Map("data" -> "emphasised title")
                        )
                      ),
                      Map("type" -> "simple")
                    )
                  ),
                  Map.empty
                ),
                IntermediateNode("text", Nil, Map("data" -> "text inside"))
              ),
              Map("level" -> "1")
            )
          )
        )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            hasSize(equalTo(3))
          )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.head.text,
              equalTo("Text with emphasised title")
            )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.tail.head.text,
              isEmptyString
            )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.tail.tail.head.text,
              equalTo("text inside")
            )
        )
      ,
      test("to_success_block.text"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "block.text",
              List(
                IntermediateNode("text", Nil, Map("data" -> "A")),
                IntermediateNode("text", Nil, Map.empty),
                IntermediateNode("text", Nil, Map("data" -> "B")),
                IntermediateNode("text", Nil, Map("other" -> "C"))
              ),
              Map.empty
            )
          )
        )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            hasSize(equalTo(1))
          )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.head.text,
              equalTo("A\nB")
            )
        )
      ,
      test("to_success_code"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "code",
              Nil,
              Map("data" -> "val x = 1", "language" -> "scala")
            )
          ),
          Map.empty
        )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            hasSize(equalTo(1))
          )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.head.text,
              equalTo("val x = 1")
            )
        )
      ,
      test("to_code_empty"):
        val document = IntermediateDocument(
          List(
            IntermediateNode("code", Nil, Map("other" -> "some code"))
          ),
          Map.empty
        )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            isEmpty
          )
        )
      ,
      test("to_success_list_unordered"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "list",
              List(
                IntermediateNode(
                  "block.list.item",
                  List(
                    IntermediateNode("text", Nil, Map("data" -> "item 1"))
                  ),
                  Map.empty
                ),
                IntermediateNode(
                  "block.list.item",
                  List(
                    IntermediateNode("text", Nil, Map("data" -> "item 2")),
                    IntermediateNode(
                      "text",
                      Nil,
                      Map("data" -> " with addition")
                    )
                  ),
                  Map.empty
                )
              ),
              Map("type" -> "unordered")
            )
          ),
          Map.empty
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
              equalTo("- item 1")
            )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.tail.head.text,
              equalTo("- item 2 with addition")
            )
        )
      ,
      test("to_success_list_ordered"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "list",
              List(
                IntermediateNode(
                  "block.list.item",
                  List(
                    IntermediateNode("text", Nil, Map("data" -> "item 1"))
                  ),
                  Map.empty
                ),
                IntermediateNode(
                  "block.list.item",
                  List(
                    IntermediateNode("text", Nil, Map("data" -> "item 2")),
                    IntermediateNode(
                      "text",
                      Nil,
                      Map("data" -> " with addition")
                    )
                  ),
                  Map.empty
                )
              ),
              Map("type" -> "ordered", "startNumber" -> "3")
            )
          ),
          Map.empty
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
              equalTo("3. item 1")
            )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.tail.head.text,
              equalTo("4. item 2 with addition")
            )
        )
      ,
      test("to_success_link"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "link",
              List(
                IntermediateNode(
                  "link.text",
                  List(
                    IntermediateNode("text", Nil, Map("data" -> "link text"))
                  ),
                  Map.empty
                )
              ),
              Map("destination" -> "https://example.com", "title" -> "title")
            )
          ),
          Map.empty
        )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            hasSize(equalTo(1))
          )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.head.text,
              equalTo("link text (https://example.com)")
            )
        )
      ,
      test("to_success_image"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "image",
              List(
                IntermediateNode(
                  "image.text",
                  List(
                    IntermediateNode("text", Nil, Map("data" -> "image text"))
                  ),
                  Map.empty
                )
              ),
              Map(
                "destination" -> "https://example.com/test.png",
                "title" -> "title"
              )
            )
          ),
          Map.empty
        )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            hasSize(equalTo(1))
          )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.head.text,
              equalTo("image text (https://example.com/test.png)")
            )
        )
      ,
      test("to_success_emphasis"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "emphasis",
              List(
                IntermediateNode("text", Nil, Map("data" -> "one")),
                IntermediateNode("text", Nil, Map("data" -> " text"))
              ),
              Map("type" -> "simple")
            ),
            IntermediateNode(
              "emphasis",
              List(
                IntermediateNode("text", Nil, Map("data" -> "another")),
                IntermediateNode("text", Nil, Map("data" -> " text"))
              ),
              Map("type" -> "strong")
            )
          ),
          Map.empty
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
              equalTo("one text")
            )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.tail.head.text,
              equalTo("another text")
            )
        )
      ,
      test("to_success_block.quote"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "block.quote",
              List(
                IntermediateNode("text", Nil, Map("data" -> "some")),
                IntermediateNode("text", Nil, Map("data" -> " quoted")),
                IntermediateNode("text", Nil, Map("data" -> " text"))
              ),
              Map.empty
            )
          ),
          Map.empty
        )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            hasSize(equalTo(1))
          )
            && hasField(
              "text",
              (d: PlainTextDocument) => d.paragraphs.head.text,
              equalTo("some quoted text")
            )
        )
      ,
      test("to_unknown_node_type"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "unknown",
              Nil,
              Map("data" -> "some data")
            )
          ),
          Map.empty
        )
        for {
          tgtDocument <- document.to
        } yield assert(tgtDocument)(
          hasField(
            "paragraphs",
            (d: PlainTextDocument) => d.paragraphs,
            isEmpty
          )
        )
    )

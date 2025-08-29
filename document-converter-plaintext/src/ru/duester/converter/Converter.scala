package ru.duester.converter

import ru.duester.converter.converter.FromIntermediateConverter
import ru.duester.converter.converter.FromIntermediateNodeConverter
import ru.duester.converter.converter.ToIntermediateConverter
import ru.duester.converter.converter.ToIntermediateNodeConverter
import ru.duester.converter.error.ConversionError
import ru.duester.converter.model.IntermediateDocument
import ru.duester.converter.model.IntermediateNode
import ru.duester.plaintext.model.Paragraph
import ru.duester.plaintext.model.PlainTextDocument
import zio.IO
import zio.ZIO

object Converter:
  /** Converts plain text to intermediate document nodewise. Every paragraph is
    * transformed in the following way:
    *
    *   - nodeType = "text",
    *   - attribute "data" = parahraph's text
    */
  given toIntermediateNodeConverter
      : ToIntermediateNodeConverter[PlainTextDocument, Paragraph]:
    def toIntermediateNodes(
        node: Paragraph,
        childrenNodes: List[IntermediateNode]
    ): List[IntermediateNode] =
      List(IntermediateNode("text", attributes = Map("data" -> node.text)))

    def getNodes(document: PlainTextDocument): List[Paragraph] =
      document.paragraphs

    def getChildrenNodes(node: Paragraph): List[Paragraph] = Nil

    def getMetadata(document: PlainTextDocument): Map[String, String] =
      document.metadata

  /** Converts intermediate document to plain text nodewise. Performs deep
    * transformation, i.e. suitable children's nodes at any level are
    * transformed.
    *
    * Suitable means:
    *
    *   - nodeType = "text",
    *   - attribute "data" is present
    */
  given fromIntermediateNodeConverter
      : FromIntermediateNodeConverter[PlainTextDocument, Paragraph]:
    def fromIntermediateNodes(
        node: IntermediateNode,
        childrenNodes: List[Paragraph]
    ): IO[ConversionError, List[Paragraph]] =
      node match
        case IntermediateNode("text", _, attributes) =>
          attributes.get("data") match
            case Some(value) => ZIO.succeed(List(Paragraph(value)))
            case None        => ZIO.succeed(Nil)
        case _ => ZIO.succeed(childrenNodes)

    def getDocument(
        nodes: List[Paragraph],
        metadata: Map[String, String]
    ): IO[ConversionError, PlainTextDocument] =
      ZIO.succeed(PlainTextDocument(nodes, metadata))

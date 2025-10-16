package ru.duester.plaintext.converter

import ru.duester.converter.converter.FromIntermediateConverter
import ru.duester.converter.converter.FromIntermediateNodeConverter
import ru.duester.converter.converter.ToIntermediateConverter
import ru.duester.converter.converter.ToIntermediateNodeConverter
import ru.duester.converter.error.ConversionError
import ru.duester.converter.model.IntermediateDocument
import ru.duester.converter.model.IntermediateNode
import ru.duester.plaintext.model.Model.map
import ru.duester.plaintext.model.Model.merge
import ru.duester.plaintext.model.Paragraph
import ru.duester.plaintext.model.PlainTextDocument
import zio.IO
import zio.ZIO

object Converter:
  /** Converts plain text to intermediate document nodewise. Every paragraph is
    * transformed in the following way:
    *
    *   - nodeType = "text",
    *   - attribute "data" = paragraph's text
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
    */
  given fromIntermediateNodeConverter
      : FromIntermediateNodeConverter[PlainTextDocument, Paragraph]:
    def fromIntermediateNodes(
        node: IntermediateNode,
        childrenNodes: List[Paragraph]
    ): IO[ConversionError, List[Paragraph]] =
      node match
        case IntermediateNode("heading", _, _) => ZIO.succeed(childrenNodes)
        case IntermediateNode("heading.title", _, _) =>
          ZIO.succeed(List(childrenNodes.merge, Paragraph("")))
        case IntermediateNode("block.text", _, _) =>
          ZIO.succeed(List(childrenNodes.merge))
        case IntermediateNode("text", _, attributes) =>
          if (attributes.isEmpty) ZIO.succeed(List(Paragraph("\n")))
          else
            attributes.get("data") match
              case Some(data) => ZIO.succeed(List(Paragraph(data)))
              case None       => ZIO.succeed(List())
        case IntermediateNode("code", _, attributes) =>
          attributes.get("data") match
            case Some(data) => ZIO.succeed(List(Paragraph(data)))
            case None       => ZIO.succeed(List())
        case IntermediateNode("list", _, attributes) =>
          (attributes.get("type"), attributes.get("startNumber")) match
            case (Some("unordered"), _) =>
              ZIO.succeed(childrenNodes.map(_.map(s => s"- $s")))
            case (Some("ordered"), Some(startNumber)) =>
              ZIO.succeed(childrenNodes.zipWithIndex.map {
                case (paragraph, index) =>
                  paragraph.map(s => s"${startNumber.toInt + index}. $s")
              })
            case _ => ZIO.succeed(List())
        case IntermediateNode("block.list.item", _, _) =>
          ZIO.succeed(List(childrenNodes.merge))
        case IntermediateNode("link", _, attributes) =>
          attributes.get("destination") match
            case Some(destination) =>
              ZIO.succeed(
                List(childrenNodes.merge.map(s => s"$s ($destination)"))
              )
            case None => ZIO.succeed(List())
        case IntermediateNode("link.text", _, _) =>
          ZIO.succeed(List(childrenNodes.merge))
        case IntermediateNode("image", _, attributes) =>
          attributes.get("destination") match
            case Some(destination) =>
              ZIO.succeed(
                List(childrenNodes.merge.map(s => s"$s ($destination)"))
              )
            case None => ZIO.succeed(List())
        case IntermediateNode("image.text", _, _) =>
          ZIO.succeed(List(childrenNodes.merge))
        case IntermediateNode("emphasis", _, _) =>
          ZIO.succeed(List(childrenNodes.merge))
        case IntermediateNode("block.quote", _, _) =>
          ZIO.succeed(List(childrenNodes.merge))
        case _ => ZIO.succeed(childrenNodes)

    def getDocument(
        nodes: List[Paragraph],
        metadata: Map[String, String]
    ): IO[ConversionError, PlainTextDocument] =
      ZIO.succeed(PlainTextDocument(nodes, metadata))

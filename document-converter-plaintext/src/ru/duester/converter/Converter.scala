package ru.duester.converter

import ru.duester.converter.converter.FromIntermediateConverter
import ru.duester.converter.converter.FromIntermediateNodeConverter
import ru.duester.converter.converter.ToIntermediateConverter
import ru.duester.converter.converter.ToIntermediateNodeConverter
import ru.duester.converter.error.ConversionError
import ru.duester.converter.model.Document
import ru.duester.converter.model.Node
import ru.duester.plaintext.model.Paragraph
import ru.duester.plaintext.model.PlainTextDocument
import zio.IO
import zio.ZIO

object Converter:
    given toIntermediate: ToIntermediateConverter[PlainTextDocument]:
        def toIntermediateDocument(document: PlainTextDocument): IO[ConversionError, Document] =
            val nodes = document.paragraphs.map { case paragraph =>
                Node("text", textContent = Some(paragraph.text))
            }
            ZIO.succeed(Document(nodes))
    
    given fromIntermediateConverter: FromIntermediateConverter[PlainTextDocument]:
        def fromIntermediateDocument(document: Document): IO[ConversionError, PlainTextDocument] =
            val paragraphs = document.nodes.collect { 
                case Node("text", Some(textContent), _, _, _) => Paragraph(textContent)
            }
            ZIO.succeed(PlainTextDocument(paragraphs))
    
    given toIntermediateNodeConverter: ToIntermediateNodeConverter[PlainTextDocument, Paragraph]:
        def toIntermediateNodes(node: Paragraph, childrenNodes: List[Node]): IO[ConversionError, List[Node]] =
            ZIO.succeed(List(Node("text", textContent = Some(node.text))))
        
        def getNodes(document: PlainTextDocument): List[Paragraph] = document.paragraphs

        def getChildrenNodes(node: Paragraph): List[Paragraph] = Nil

        def getMetadata(document: PlainTextDocument): Map[String, String] = document.metadata
    
    given fromIntermediateNodeConverter: FromIntermediateNodeConverter[PlainTextDocument, Paragraph]:
        def fromIntermediateNodes(node: Node, childrenNodes: List[Paragraph]): IO[ConversionError, List[Paragraph]] =
            node match
                case Node("text", Some(textContent), _, _, _) => ZIO.succeed(List(Paragraph(textContent)))
                case _ => ZIO.succeed(Nil)
        
        def getDocument(nodes: List[Paragraph], metadata: Map[String, String]): PlainTextDocument = 
            PlainTextDocument(nodes, metadata)

package ru.duester.converter

import ru.duester.converter.converter.FromIntermediateConverter
import ru.duester.converter.converter.ToIntermediateConverter
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
            ZIO.succeed(PlainTextDocument(paragraphs) )

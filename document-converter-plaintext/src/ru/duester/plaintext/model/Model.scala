package ru.duester.plaintext.model

object Model:
  extension (paragraphs: List[Paragraph])
    def merge: Paragraph =
      val content = paragraphs.map(_.text).mkString("")
      Paragraph(content)

  extension (paragraph: Paragraph)
    def map(f: String => String): Paragraph =
      Paragraph(f(paragraph.text))

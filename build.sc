//| mill-version: 1.0.3

import mill.*
import mill.scalalib.*
import mill.scalalib.publish.*

object `document-converter-plaintext` extends ScalaModule, SonatypeCentralPublishModule {
  def scalaVersion = "3.7.1"

  def publishVersion = "0.0.1-SNAPSHOT"

  def repositories = Seq(
    "https://central.sonatype.com/repository/maven-snapshots"
  )

  def mvnDeps = Seq(
    mvn"dev.zio::zio:2.1.20",
    mvn"io.github.duester::plaintext:0.0.2-SNAPSHOT",
    mvn"io.github.duester::document-converter:0.0.1-SNAPSHOT"
  )

  object test extends ScalaTests, TestModule.ZioTest {
    def mvnDeps = Seq(
      mvn"dev.zio::zio-test:2.1.20"
    )
  }

  def pomSettings = PomSettings(
    description = "Converter plain text and intermediary document format",
    organization = "io.github.duester",
    url = "https://github.com/duester/document-converter-plaintext",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("duester", "document-converter-plaintext"),
    developers =
      Seq(Developer("duester", "Maxim Duester", "https://github.com/duester"))
  )
}

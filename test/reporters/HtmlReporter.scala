/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reporters

import org.scalatest.Reporter
import org.scalatest.events.{Event, RunCompleted, RunStarting, TestFailed}
import play.api.libs.json.{JsObject, JsValue, Json, OFormat}
import uk.gov.hmrc.scalatestaccessibilitylinter.domain.{AccessibilityLinter, AccessibilityViolation, FailedAccessibilityChecks}

import java.io.{File, PrintWriter}
import java.security.MessageDigest
import scala.io.Source
import scala.reflect.io.Path.jfile2path

class HtmlReporter extends Reporter {
  val combinedJson = "combined.json"
  val htmlTemplate = "template.html"
  val jsonTestFolder = "test-json"
  val staticWebFolder = "static"
  val targetAccessibilityReportPath = "target/accessibility-report/"
  val intellijDebuggerURL = "http://localhost:63342/accessibility-linter-example-frontend/"

  def md5(content: String): String = MessageDigest.getInstance("MD5").digest(content.getBytes).map(0xFF & _)
    .map { "%02x".format(_) }.foldLeft("") {_ + _}

  def printViolationDirectLink(violationsJson: Array[JsObject]): Unit = {
    violationsJson.foreach { violation => {
        val uuid = (violation \ "uuid").as[String]
        val linter = (violation \ "linter").as[String]
        val testSuiteName = (violation \ "testSuiteName").as[String]
        val description = (violation \ "description").as[String].replaceAll("\n", "").replaceAll("\r", "")

        println(s"View ($linter) violation in $testSuiteName: $description\n" +
          s"http://localhost:63342/accessibility-linter-example-frontend/target/accessibility-report/template.html?search=$uuid")
      }
    }
  }

  def createTestedHtmlFiles(violationsJson: Array[JsObject]): Unit = {
    violationsJson.foreach { violation => {
        val testedHtml = (violation \ "testedHtml").as[String]
        val testSuiteName = (violation \ "testSuiteName").as[String]

        val htmlCode = new File(s"$targetAccessibilityReportPath$staticWebFolder/${testSuiteName}.html")
        if(!htmlCode.exists()) {
          htmlCode.createNewFile()
          new PrintWriter(htmlCode.path) { write(testedHtml); close() }
        }
      }
    }
  }

  def fileContent(file: File): String = {
    val source = Source.fromFile(file)
    val content = source.mkString
    source.close()
    content
  }

  override def apply(event: Event): Unit = {
    import HtmlReporter._

    import sys.process._

    event match {
      case _:RunStarting =>
        s"cp -r accessibility-report-app/. $targetAccessibilityReportPath" !
      case _:RunCompleted =>
        s"cp -r target/$jsonTestFolder/. $targetAccessibilityReportPath$staticWebFolder" !

        val directory = new File(s"$targetAccessibilityReportPath$staticWebFolder")

        val accessibilityViolations: Seq[JsObject] = directory.listFiles().flatMap((f: File) => {
          Json.parse(fileContent(f)) \\ "violations"
        }).foldLeft(Seq.empty[JsObject])((arr, a) => arr ++ a.as[List[JsObject]].map(b => b))

        val templateJson = new File(s"$targetAccessibilityReportPath$staticWebFolder/$combinedJson")
        templateJson.createFile()
        val allViolationsJson = Json.obj("violations" -> accessibilityViolations)
        new PrintWriter(templateJson.path) { write(Json.prettyPrint(allViolationsJson)); close() }

        createTestedHtmlFiles(accessibilityViolations.toArray)

        println(s"\nView full accessibility html test report: $intellijDebuggerURL$htmlTemplate")
      case testFailed: TestFailed =>
        val testedHtml: String = testFailed.payload.getOrElse("").asInstanceOf[String]
        testFailed.recordedEvents.foreach(e => e.payload match {
          case Some(FailedAccessibilityChecks(_, violations)) =>
            val json = violations.map(v => {
              val uuid = md5(Json.stringify(Json.toJson(v)))
              Json.toJson(v).as[JsObject] +
                ("testSuiteName" -> Json.toJson(testFailed.suiteName)) +
                ("uuid" -> Json.toJson(uuid)) +
                ("testedHtml" -> Json.toJson(testedHtml)) +
                ("testedHtmlPath" -> Json.toJson(s"./$staticWebFolder/${testFailed.suiteName}.html"))
            })

            val directory = new File(s"target/$jsonTestFolder")
            if(!directory.isDirectory) directory.mkdir()
            val file = new File(s"${directory.getPath}/${testFailed.suiteName}.json")
            if(!file.exists) {
              file.createFile()

              val violation = Violation(testFailed.suiteName, testFailed.suiteClassName, testFailed.duration, json)
              new PrintWriter(file.path) { write(Json.prettyPrint(Json.toJson(violation))); close() }

              printViolationDirectLink(json.toArray)
            } else {
              val jsonStr = fileContent(file)
              val jsonContent = Json.parse(jsonStr)
              val existingViolations = (jsonContent \ "violations").as[List[JsObject]]
              val existingViolationUUIDs = existingViolations.map(e => (e \ "uuid").as[String])
              val newViolations = json.toArray.filter(v => !existingViolationUUIDs.contains((v \ "uuid").as[String]))

              if(newViolations.length > 0) {
                val appendNewViolations = existingViolations ++ newViolations
                val violation = Violation(testFailed.suiteName, testFailed.suiteClassName, testFailed.duration, appendNewViolations)
                new PrintWriter(file.path) { write(Json.prettyPrint(Json.toJson(violation))); close() }

                printViolationDirectLink(newViolations)
              }
            }
          case _ =>
        })
      case _ =>
    }
  }
}

object HtmlReporter {

  import AccessibilityLinter._
  import play.api.libs.json._

  implicit val axeFormat: OFormat[axe.type] = Json.format[axe.type]
  implicit val vnuFormat: OFormat[vnu.type] = Json.format[vnu.type]
  implicit val accessibilityLinterFormat: Format[AccessibilityLinter] = new Format[AccessibilityLinter] {
    override def writes(o: AccessibilityLinter): JsValue = o match {
      case a: axe.type => Json.toJson(a.toString)
      case a: vnu.type => Json.toJson(a.toString)
    }
    override def reads(json: JsValue): JsResult[AccessibilityLinter] =
      axeFormat.reads(json).orElse(
        vnuFormat.reads(json))
  }

  implicit val accessibilityViolationFormat: Format[AccessibilityViolation] = Json.format[AccessibilityViolation]
  implicit val failedAccessibilityFormat: Format[FailedAccessibilityChecks] = Json.format[FailedAccessibilityChecks]
}

case class Violation(testSuiteName: String, testSuiteClassName: Option[String], runDuration: Option[Long], violations: Seq[JsObject])

object Violation {
  implicit val violationFormat: OFormat[Violation] = Json.format[Violation]
}



import org.scalatest._
import funspec._
import matchers._
import Inspectors._
import better.files._
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers

class ExamplePagesFromRealServicesSpec extends AnyFunSpec with should.Matchers with AccessibilityMatchers {
  describe("examples from real services") {
    forAll(File("a11y/example-pages-from-real-services").children.toList) { example =>
      describe(example.name) {
        it("passes accessibility checks") {
          example.contentAsString should passAccessibilityChecks
        }
      }
    }
  }
}

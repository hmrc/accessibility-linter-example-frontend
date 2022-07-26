/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.Payloads.withPayload
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n._
import play.api.test.FakeRequest
import uk.gov.hmrc.accessibilitylinterexamplefrontend.views.html.HelloWorldPage
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers

class HelloWorldPageSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with AccessibilityMatchers {
  private val request      = FakeRequest("GET", "/")
  private val messages     = app.injector.instanceOf[MessagesApi].preferred(request)
  private val pageTemplate = app.injector.instanceOf[HelloWorldPage]

  "HelloUniversePage" must {
    "pass accessibility checks" in {
      val pageHtml = pageTemplate.render(request, messages).toString()

      withPayload(pageHtml) { pageHtml must passAccessibilityChecks }
    }
  }
}

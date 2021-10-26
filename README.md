# Accessibility Linter Example Frontend

Created to support a platform demo presentation

This repository contains an example of how to use https://github.com/hmrc/sbt-accessibility-linter

The page we're testing is accessible after an `sbt run` at http://localhost:9000/

The template is [HelloWorldPage.scala.html](app/uk/gov/hmrc/accessibilitylinterexamplefrontend/views/HelloWorldPage.scala.html)

Changes that introduce the linter: https://github.com/hmrc/accessibility-linter-example-frontend/pull/1/files

Run the accessibility tests with `sbt a11yTest`

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
#!/bin/bash

# Codacy setup. https://github.com/codacy/codacy-coverage-reporter#build-from-source
# Manually downloading the latest version from the Maven Central, since the jpm4j stopped working.
curl -sL  http://repo1.maven.org/maven2/com/codacy/codacy-coverage-reporter/1.0.13/codacy-coverage-reporter-1.0.13-assembly.jar >> codacy-coverage-reporter.jar
java -cp ./codacy-coverage-reporter.jar com.codacy.CodacyCoverageReporter -l Java -r ./build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml
# END of Codacy setup.

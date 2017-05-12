#!/bin/bash

# This script uploads the Travis artifacts to Google Cloud Storage.

# Installation of https://github.com/travis-ci/dpl.
gem instal dpl
# Prepare the test and coverage reports for the upload.
mkdir reports
zip -r reports/test-reports.zip **/build/reports
zip -r reports/jacoco-reports.zip **/build/jacoco
# Upload the prepared reports to GCS.
dpl --provider=gcs \
    --access-key-idGOOGX66ER6DXLZH7IKQF \
    --secret-access-key=${GCS_SECRET} \
    --bucket=spine-dev.appspot.com \
    --upload-dir=core-java/builds/${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH}-${TRAVIS_BUILD_NUMBER} \
    --local-dir=reports \
    --skip_cleanup=true

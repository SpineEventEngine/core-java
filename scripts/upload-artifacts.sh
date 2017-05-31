#!/bin/bash

# This script uploads the Travis artifacts to Google Cloud Storage.

# Installation of https://github.com/travis-ci/dpl.
gem instal dpl
# Prepare the test and coverage reports for the upload.
mkdir reports
zip -r reports/test-reports.zip **/build/reports
zip -r reports/jacoco-reports.zip **/build/jacoco

# Returns the value for the specified key.
function getProp() {
    grep "${1}" gcs.properties | cut -d'=' -f2
}

# Upload the prepared reports to GCS.
dpl --provider=gcs \
    --access-key-id=GOOGX66ER6DXLZH7IKQF \
    --secret-access-key=${GCS_SECRET} \
    --bucket="$(getProp 'artifacts.bucket')" \
    --upload-dir="$(getProp 'artifacts.folder')"/${TRAVIS_BUILD_NUMBER}-${TRAVIS_PULL_REQUEST_BRANCH:-$TRAVIS_BRANCH} \
    --local-dir=reports \
    --skip_cleanup=true

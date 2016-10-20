#!/bin/bash

# This script uses Travis `TRAVIS_BRANCH` environment variable to check if the artifacts should be published.
#
# If the branch is `master`, script triggers the publishing process.
# Tests are skipped during the publishing, as the script should be executed on `after_success` step of
# the build lifecycle.

echo " -- PUBLISHING: current branch is $TRAVIS_BRANCH."

if [ $TRAVIS_BRANCH == 'master' ]; then
    echo " ------ Publishing the artifacts to the repository..."
    openssl aes-256-cbc -K $encrypted_0f12c1faf1fc_key -iv $encrypted_0f12c1faf1fc_iv -in credentials.properties.enc -out credentials.properties -d
    ./gradlew publish -x test
    echo " ------ Artifacts published."
else
    echo " ------ Publishing is DISABLED for the current branch."
fi

echo " -- PUBLISHING: completed."

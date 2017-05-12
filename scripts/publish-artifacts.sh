#!/bin/bash

# This script uses Travis `TRAVIS_BRANCH` environment variable to check if the artifacts should be published.
#
# If the branch is `master`, script triggers the publishing process.
# Tests are skipped during the publishing, as the script should be executed on `after_success` step of
# the build lifecycle.

echo " -- PUBLISHING: current branch is $TRAVIS_BRANCH."

if [ "$TRAVIS_BRANCH" == 'master' ] && [ "$TRAVIS_PULL_REQUEST" = "false" ]; then
    echo " ------ Publishing the artifacts to the repository..."
    ./gradlew publish -x test
    echo " ------ Artifacts published."
else
    echo " ------ Publishing is DISABLED for the current branch."
fi

echo " -- PUBLISHING: completed."

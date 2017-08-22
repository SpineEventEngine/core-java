#!/bin/bash

# This script uses Travis environment variables to check if the artifacts should be published.
#
# If the build is successful and the branch is `master`, script triggers the publishing process.
#
# Tests are skipped during the publishing, as the script should be executed after their execution.

echo " -- PUBLISHING: current branch is $TRAVIS_BRANCH."

if [ "$TRAVIS_TEST_RESULT" == 1 ]; then
    echo " ------ The build is broken. Publishing will not be performed."
    exit 0
fi

if [ "$TRAVIS_BRANCH" != "master" ] || [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    echo " ------ Publishing is DISABLED for the current branch."
    exit 0
fi

echo " ------ Publishing the artifacts to the repository..."
./gradlew publish -x test
echo " ------ Artifacts published."

echo " -- PUBLISHING: completed."

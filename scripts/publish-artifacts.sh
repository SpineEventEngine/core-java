#!/bin/bash

echo " -- PUBLISHING: current branch is $TRAVIS_BRANCH."

if [ $TRAVIS_BRANCH == 'master' ]; then
    echo " ------ Publishing the artifacts to the repository..."
    ./gradlew publish -x test
    echo " ------ Artifacts published."
else
    echo " ------ Publishing is DISABLED for the current branch."
fi

echo " -- PUBLISHING: completed."

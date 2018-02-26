#!/bin/bash

readonly SOURCE_REPOSITORY=https://api.github.com/repos/SpineEventEngine/core-java
readonly SOURCE_FILE_WITH_VERSION='ext.gradle'
readonly SOURCE_VERSION_VARIABLE='SPINE_VERSION'

readonly TARGET_REPOSITORY=https://api.github.com/repos/SpineEventEngine/jdbc-storage
readonly TARGET_FILE_WITH_VERSION='ext.gradle'
readonly TARGET_VERSION_VARIABLE='coreVersion'

readonly NEW_BRANCH_NAME='update-core-version'
readonly BRANCH_TO_MERGE_INTO='master'

readonly COMMIT_MESSAGE='Update the version of Spine core modules'
readonly PULL_REQUEST_TITLE='Update the version of Spine core modules'
readonly PULL_REQUEST_BODY='This is auto-generated PR.
    It was created because the version of the Spine core library was updated in its recent build.
    This PR makes the Spine jdbc storage library dependant on the latest version of the Spine core.'
readonly PULL_REQUEST_ASSIGNEE='armiol'

readonly STATUS_CHECK_TIMEOUT_SECONDS=1200

function main() {
    local gitAuthorizationToken="$1"

    chmod +x ./scripts/dependant-repositories/update-dependency-version.sh
    ./scripts/dependant-repositories/update-dependency-version.sh \
        "${gitAuthorizationToken}" \
        "${SOURCE_REPOSITORY}" \
        "${SOURCE_FILE_WITH_VERSION}" \
        "${SOURCE_VERSION_VARIABLE}" \
        "${TARGET_REPOSITORY}" \
        "${TARGET_FILE_WITH_VERSION}" \
        "${TARGET_VERSION_VARIABLE}" \
        "${NEW_BRANCH_NAME}" \
        "${BRANCH_TO_MERGE_INTO}" \
        "${COMMIT_MESSAGE}" \
        "${PULL_REQUEST_TITLE}" \
        "${PULL_REQUEST_BODY}" \
        "${PULL_REQUEST_ASSIGNEE}" \
        "${STATUS_CHECK_TIMEOUT_SECONDS}"
}

main "$@"
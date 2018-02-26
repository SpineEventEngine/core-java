#!/bin/bash
#
# Update core library dependency of the gae-java repository to the latest version.

readonly SOURCE_REPOSITORY=https://api.github.com/repos/SpineEventEngine/core-java
readonly SOURCE_FILE_WITH_VERSION='ext.gradle'
readonly SOURCE_VERSION_VARIABLE='SPINE_VERSION'

readonly TARGET_REPOSITORY=https://api.github.com/repos/SpineEventEngine/gae-java
readonly TARGET_FILE_WITH_VERSION='ext.gradle'
readonly TARGET_VERSION_VARIABLE='spineVersion'

readonly NEW_BRANCH_NAME='update-core-version'
readonly BRANCH_TO_MERGE_INTO='master'

readonly COMMIT_MESSAGE='Update the version of Spine core modules'
readonly PULL_REQUEST_TITLE='Update the version of Spine core modules'
readonly PULL_REQUEST_BODY='Auto-generated PR to update core library dependency to the latest version.'
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
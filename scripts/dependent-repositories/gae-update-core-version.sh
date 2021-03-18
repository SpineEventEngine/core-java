#!/bin/bash

#
# Copyright 2021, TeamDev. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Redistribution and use in source and/or binary forms, with or without
# modification, must retain the above copyright notice and the following
# disclaimer.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

#
# Update the core library dependency of the `gae-java` repository to the latest version.

readonly SOURCE_REPOSITORY=https://api.github.com/repos/SpineEventEngine/core-java
readonly SOURCE_FILE_WITH_VERSION='ext.gradle'
readonly SOURCE_VERSION_VARIABLE='SPINE_VERSION'

readonly TARGET_REPOSITORY=https://api.github.com/repos/SpineEventEngine/gae-java
readonly TARGET_FILE_WITH_VERSION='ext.gradle'
readonly TARGET_VERSION_VARIABLE='spineVersion'
readonly TARGET_COMMIT_MESSAGE="Update the dependency to \`core-java\` *version*."

readonly OWN_FILE_WITH_VERSION='ext.gradle'
readonly OWN_VERSION_VARIABLE='spineGaeVersion'
readonly OWN_COMMIT_MESSAGE="Update the library version to *version*."

readonly NEW_BRANCH_NAME='update-to-core-java-*version*'
readonly BRANCH_TO_MERGE_INTO='master'

readonly PULL_REQUEST_TITLE="Update the dependency to \`core-java\` *version*."
readonly PULL_REQUEST_BODY='Auto-generated PR to update core library dependency to the latest version.'
readonly PULL_REQUEST_ASSIGNEE='armiol'

function main() {
    local gitAuthorizationToken="$1"

    chmod +x ./scripts/dependent-repositories/update-dependency-version.sh
    ./scripts/dependent-repositories/update-dependency-version.sh \
        "${gitAuthorizationToken}" \
        "${SOURCE_REPOSITORY}" \
        "${SOURCE_FILE_WITH_VERSION}" \
        "${SOURCE_VERSION_VARIABLE}" \
        "${TARGET_REPOSITORY}" \
        "${TARGET_FILE_WITH_VERSION}" \
        "${TARGET_VERSION_VARIABLE}" \
        "${NEW_BRANCH_NAME}" \
        "${BRANCH_TO_MERGE_INTO}" \
        "${TARGET_COMMIT_MESSAGE}" \
        "${PULL_REQUEST_TITLE}" \
        "${PULL_REQUEST_BODY}" \
        "${PULL_REQUEST_ASSIGNEE}"

    # As the branch already exists, the second script call will push files directly there.
    ./scripts/dependent-repositories/update-dependency-version.sh \
        "${gitAuthorizationToken}" \
        "${SOURCE_REPOSITORY}" \
        "${SOURCE_FILE_WITH_VERSION}" \
        "${SOURCE_VERSION_VARIABLE}" \
        "${TARGET_REPOSITORY}" \
        "${OWN_FILE_WITH_VERSION}" \
        "${OWN_VERSION_VARIABLE}" \
        "${NEW_BRANCH_NAME}" \
        "${BRANCH_TO_MERGE_INTO}" \
        "${OWN_COMMIT_MESSAGE}" \
        "${PULL_REQUEST_TITLE}" \
        "${PULL_REQUEST_BODY}" \
        "${PULL_REQUEST_ASSIGNEE}"
}

main "$@"

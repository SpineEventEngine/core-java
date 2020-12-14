#!/bin/bash

#
# Copyright 2020, TeamDev. All rights reserved.
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
# Update the core library dependency version in the multiple repositories.
#
# This script accepts a single command line argument - "gitAuthorizationToken".
# Such token can be generated in the "Settings/Developer settings/Personal access tokens" page on the GitHub website.
# It allows to perform all requests to the GitHub API on behalf of the token owner.
#
# To store such token for the usage in the ".travis.yml" file, call 'travis encrypt GITHUB_TOKEN=*your token*'.
# Then copy-paste the result in the "env: global" section of the ".travis.yml" in place of the old encrypted token.
# The decrypted version of the token will be available in the GITHUB_TOKEN environment variable during the build.

function main() {
    local gitAuthorizationToken="$1"

    chmod +x ./scripts/dependent-repositories/gae-update-core-version.sh
    ./scripts/dependent-repositories/gae-update-core-version.sh \
        "${gitAuthorizationToken}"

    chmod +x ./scripts/dependent-repositories/jdbc-update-core-version.sh
    ./scripts/dependent-repositories/jdbc-update-core-version.sh \
        "${gitAuthorizationToken}"
}

main "$@"

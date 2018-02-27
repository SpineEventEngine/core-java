#!/bin/bash
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

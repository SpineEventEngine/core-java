#!/bin/bash
#
# Make all repositories dependant on the core library have its latest version as the dependency.

function main() {
    local gitAuthorizationToken="$1"

    chmod +x ./scripts/dependant-repositories/gae-update-core-version.sh
    ./scripts/dependant-repositories/gae-update-core-version.sh \
        "${gitAuthorizationToken}"

    chmod +x ./scripts/dependant-repositories/jdbc-update-core-version.sh
    ./scripts/dependant-repositories/jdbc-update-core-version.sh \
        "${gitAuthorizationToken}"
}

main "$@"

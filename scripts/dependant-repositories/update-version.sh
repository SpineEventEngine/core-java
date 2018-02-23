#!/bin/bash

# Input repository parameters
readonly INPUT_FILE_PATH=https://api.github.com/repos/dmitrykuzmin/chat/contents/testfileinput.gradle
readonly INPUT_VERSION_LABEL='TEST_VERSION'

# Repository 1 parameters
readonly REPOSITORY_1_URL='https://api.github.com/repos/dmitrykuzmin/chat'
readonly FILE_WITH_VERSION_1_PATH='testfileoutput1.gradle'
readonly BRANCH_TO_CREATE_1='update-1'
readonly REPOSITORY_1_DEFAULT_BRANCH='master'
readonly FILE_1_VERSION_LABEL='toBeSetVersion'

# Repository 2 parameters
readonly REPOSITORY_2_URL='https://api.github.com/repos/dmitrykuzmin/chat'
readonly FILE_WITH_VERSION_2_PATH='testfileoutput2.gradle'
readonly BRANCH_TO_CREATE_2='update-2'
readonly REPOSITORY_2_DEFAULT_BRANCH='master'
readonly FILE_2_VERSION_LABEL='inputVersion'

# Common
readonly COMMIT_MESSAGE='Test commit'
readonly PULL_REQUEST_TITLE='Test PR'
readonly PULL_REQUEST_BODY='Test'
readonly PULL_REQUEST_ASSIGNEE='dmitrykuzmin'

# Github token (need to pass as cmd)
readonly GITHUB_TOKEN='test token'

# Status checks timeout for PR is 20 minutes, then script abandons the idea to merge it.
readonly STATUS_CHECKS_TIMEOUT_SECONDS=1200

# In the following functions we use 'echo' to stdout and command substitution on call to emulate return value.

search_for_string() {
    local string="$1"
    local text="$2"
    echo "$(grep "${string}" <<< "${text}")"
}

# This function demands that the target string is on the left part of the assignment.
search_for_string_assignment() {
    local string="$1"
    local text="$2"

    # Only search strings with equal sign after them.
    local patternToSearch="${string} *= *"

    echo "$(search_for_string "${patternToSearch}" "${text}")"
}

read_json_field() {
    local fieldName="$1"
    local jsonData="$2"

    echo "$(grep -o -m 1 '"'${fieldName}'": *"[^"]*"' <<< "${jsonData}" \
        | grep -o '"[^"]*"$')"
}

# Numeric json fields don't have quotes, so they need different parsing method.
read_json_field_numeric() {
    local fieldName="$1"
    local jsonData="$2"

    echo "$(grep -o '"'${fieldName}'": *[0-9]*' <<< "${jsonData}" \
        | grep -o '[0-9]*$')"
}

read_file_content() {
    local fileData="$1"

    # Read file's base64-encoded content.
    local fileContent="$(read_json_field 'content' "${fileData}")"

    # Remove all line separators from the encoded data.
    fileContent="${fileContent//\\n/}"

    # Remove quotes from the encoded data.
    fileContent="${fileContent//\"/}"

    echo "${fileContent}"
}

obtain_file_sha() {
    local fileData="$1"
    echo "$(read_json_field 'sha' "${fileData}")"
}

retrieve_labeled_value() {
    local label="$1"
    local data="$2"

    echo "$(tr -d '\n' <<< "${data}" | sed -e 's/.*'"${label}"'//')"
}

check_status_code() {
    local code="$1"
    local expectedValue="$2"

    if [ "${code}" = "${expectedValue}" ]; then
        echo 'true'
    else
        echo 'false'
    fi
}

encode() {
    local text="$1"
    echo "$(base64 --wrap=0 <<< "${text}")"
}

decode() {
    local text="$1"
    echo "$(base64 -d <<< "${text}")"
}

remove_label() {
    local label="$1"
    local stringWithVersion="$2"

    # Account for all spaces and "equal" sign after the label.
    labelString="$(grep -o "${label}"'[ =]*' <<< "${stringWithVersion}")"

    # Remove the label string and everything preceding it.
    local patternToRemove="*${labelString}"

    echo "${stringWithVersion##${patternToRemove}}"
}

remove_version() {
    local version="$1"
    local stringWithVersion="$2"

    local versionSubstringStart="$(grep -b -o "$version" <<< "$stringWithVersion" \
                                    | cut -d: -f1)"

    echo "${stringWithVersion:0:${versionSubstringStart}}"
}

substitute_version() {
    local oldVersion="$1"
    local newVersion="$2"
    local versionLabel="$3"
    local text="$4"

    local oldStringWithVersion="${versionLabel}${oldVersion}"
    local newStringWithVersion="${versionLabel}${newVersion}"
    local newText="$(sed "s/${oldStringWithVersion}/${newStringWithVersion}/g" \
        <<< "${text}")"

    echo "${newText}"
}

check_branch_exists() {
    local branchName="$1"
    local repositoryUrl="$2"

    local httpStatusLabel='HTTP_STATUS:'

    local responseBody="$(curl -H "Authorization: token ${GITHUB_TOKEN}" \
        --write-out "${httpStatusLabel}%{http_code}" \
        "${repositoryUrl}/git/refs/heads/${branchName}")"

    local branchRequestStatusCode="$(retrieve_labeled_value "${httpStatusLabel}" \
        "${responseBody}")"

    echo "$(check_status_code "${branchRequestStatusCode}" '200')"
}

create_branch() {
    local branchName="$1"
    local defaultBranchName="$2"
    local repositoryUrl="$3"

    local repositoryRefsUrl="${repositoryUrl}/git/refs"

    local masterBranchData="$(curl -H "Authorization: token ${GITHUB_TOKEN}" \
        "${repositoryRefsUrl}/heads/${defaultBranchName}")"

    local lastCommitSha="$(read_json_field 'sha' "${masterBranchData}")"

    local requestData="{\"ref\":\"refs/heads/${branchName}\",
                        \"sha\":${lastCommitSha}}"

    curl -H "Authorization: token ${GITHUB_TOKEN}" \
        -H "Content-Type: application/json" \
        --data "${requestData}" \
        "${repositoryRefsUrl}"
}

delete_branch() {
    local branchName="$1"
    local repositoryUrl="$2"

    curl -H "Authorization: token ${GITHUB_TOKEN}" \
        -X DELETE \
        "${repositoryUrl}/git/refs/heads/${branchName}"
}

get_status_check_result() {
    local branchName="$1"
    local repositoryUrl="$2"

    local statusData="$(curl -H "Authorization: token ${GITHUB_TOKEN}" \
        "${repositoryUrl}/commits/heads/${branchName}/status")"

    local statusCheckResult="$(read_json_field 'state' "${statusData}")"

    # Remove all quotes from the result.
    statusCheckResult="${statusCheckResult//\"/}"

    echo "${statusCheckResult}"
}

get_file_data() {
    local fileUrl="$1"
    echo "$(curl -H "Authorization: token ${GITHUB_TOKEN}" "${fileUrl}")"
}

commit_file() {
    local commitMessage="$1"
    local filePath="$2"
    local newFileContents="$3"
    local fileSha="$4"
    local branch="$5"

    local requestData="{\"message\":\"${commitMessage}\",
                        \"content\":\"${newFileContents}\",
                        \"sha\":${fileSha},
                        \"branch\":\"${branch}\"}"

    curl -H "Authorization: token ${GITHUB_TOKEN}" \
        -H "Content-Type: application/json" \
        -X PUT \
        --data "${requestData}" \
        "${filePath}"
}

create_pull_request() {
    local pullRequestName="$1"
    local head="$2"
    local base="$3"
    local body="$4"
    local repositoryUrl="$5"

    local requestData="{\"title\":\"${pullRequestName}\",
                        \"head\":\"${head}\",
                        \"base\":\"${base}\",
                        \"body\":\"${body}\"}"

    local creationResponse="$(curl -H "Authorization: token ${GITHUB_TOKEN}" \
        -H "Content-Type: application/json" \
        --data "$requestData" \
        "${repositoryUrl}/pulls")"

    local pullRequestNumber="$(read_json_field_numeric \
        'number' \
        "${creationResponse}")"

    echo "${pullRequestNumber}"
}

assign_pull_request() {
    local pullRequestNumber="$1"
    local repositoryUrl="$2"
    local assignee="$3"

    local requestData="{\"assignees\":[\"${assignee}\"]}"

    curl -H "Authorization: token ${GITHUB_TOKEN}" \
        -H "Content-Type: application/json" \
        -X PATCH \
        --data "${requestData}" \
        "${repositoryUrl}/issues/${pullRequestNumber}"
}

merge_pull_request() {
    local pullRequestNumber="$1"
    local repositoryUrl="$2"

    local httpStatusLabel='HTTP_STATUS:'

    local mergeResponse="$(curl -H "Authorization: token ${GITHUB_TOKEN}" \
        -H "Content-Type: application/json" \
        -X PUT \
        --write-out "${httpStatusLabel}%{http_code}" \
        "${repositoryUrl}/pulls/${pullRequestNumber}/merge")"

    local mergeRequestStatusCode="$(retrieve_labeled_value "${httpStatusLabel}" \
        "${mergeResponse}")"

    echo "$(check_status_code "${mergeRequestStatusCode}" '200')"
}

obtain_version() {
    local fullFilePath="$1"
    local versionLabel="$2"

    local fileData="$(get_file_data ${fullFilePath})"
    local fileContent="$(read_file_content "${fileData}")"
    local fileContentDecoded="$(decode "${fileContent}")"

    local stringWithVersion="$(search_for_string_assignment "${versionLabel}" \
        "${fileContentDecoded}")"
    local inputVersion="$(remove_label "${versionLabel}" "${stringWithVersion}")"

    echo "${inputVersion}"
}

update_version() {
    local inputVersion="$1"
    local repositoryUrl="$2"
    local filePath="$3"
    local versionLabel="$4"
    local branch="$5"
    local defaultBranch="$6"

    local fullFilePath="${repositoryUrl}/contents/${filePath}"

    local branchExists="$(check_branch_exists "${branch}" "${repositoryUrl}")"

    if [ "${branchExists}" = 'true' ]; then
        fullFilePath="${fullFilePath}?ref=${branch}"
    fi

    local fileData="$(get_file_data ${fullFilePath})"
    local fileContent="$(read_file_content "${fileData}")"
    local fileContentDecoded="$(decode "${fileContent}")"

    local stringWithVersion="$(search_for_string_assignment "${versionLabel}" \
        "${fileContentDecoded}")"

    local version="$(remove_label "${versionLabel}" "${stringWithVersion}")"

    if [ "${inputVersion}" != "${version}" ]; then

        if [ "${branchExists}" = 'false' ]; then
            create_branch "${branch}" "${defaultBranch}" "${repositoryUrl}"
        fi

        local labelString="$(remove_version "${version}" "${stringWithVersion}")"
        local newText="$(substitute_version "${version}" \
            "${inputVersion}" \
            "${labelString}" \
            "${fileContentDecoded}")"

        local encodedNewText="$(encode "${newText}")"
        local fileSha="$(obtain_file_sha "${fileData}")"
        commit_file "${COMMIT_MESSAGE}" \
            "${fullFilePath}" \
            "${encodedNewText}" \
            "${fileSha}" \
            "${branch}"

        if [ "$branchExists" = false ]; then
            local pullRequestNumber="$(create_pull_request "${PULL_REQUEST_TITLE}" \
                "${branch}" \
                "${defaultBranch}" \
                "${PULL_REQUEST_BODY}" \
                "${repositoryUrl}")"

            local statusCheckResult='pending'

            echo 'Requesting status checks result...'
            local secondsElapsed=0

            # Emulate do...while loop.
            while true; do
                statusCheckResult="$(get_status_check_result \
                    "${branch}" \
                    "${repositoryUrl}")"

                [ "${statusCheckResult}" = 'pending' ] && \
                    [ ${secondsElapsed} -lt ${STATUS_CHECKS_TIMEOUT_SECONDS} ] \
                    || break

                # Wait before the next request.
                local secondsToWait=10
                sleep ${secondsToWait}

                # Use arithmetic context for integer addition.
                secondsElapsed=$((${secondsElapsed} + 10))
            done

            echo "Status checks result: ${statusCheckResult}"

            if [ "${statusCheckResult}" = 'success' ]; then
                local mergeSuccessful="$(merge_pull_request "${pullRequestNumber}" \
                "${repositoryUrl}")"
                if [ "${mergeSuccessful}" = 'true' ]; then
                    delete_branch "${branch}" "${repositoryUrl}"
                    return 0
                fi
            fi

            # If status checks indicated failure or merge was unsuccessful assign the PR to the assignee.
            assign_pull_request "${pullRequestNumber}" \
                        "${repositoryUrl}" \
                        "${PULL_REQUEST_ASSIGNEE}"
        fi
    fi
}

main() {
    inputVersion="$(obtain_version "${INPUT_FILE_PATH}" "${INPUT_VERSION_LABEL}")"

    update_version "${inputVersion}" \
        "${REPOSITORY_1_URL}" \
        "${FILE_WITH_VERSION_1_PATH}" \
        "${FILE_1_VERSION_LABEL}" \
        "${BRANCH_TO_CREATE_1}" \
        "${REPOSITORY_1_DEFAULT_BRANCH}"

    update_version "${inputVersion}" \
        "${REPOSITORY_2_URL}" \
        "${FILE_WITH_VERSION_2_PATH}" \
        "${FILE_2_VERSION_LABEL}" \
        "${BRANCH_TO_CREATE_2}" \
        "${REPOSITORY_2_DEFAULT_BRANCH}"
}

main "$@"

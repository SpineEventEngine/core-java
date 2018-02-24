#!/bin/bash

GITHUB_REQUEST_HEADER=''

# In the following functions we use 'echo' to stdout and command substitution on call to emulate return value.

obtain_full_file_path() {
    local relativeFilePath="$1"
    local repositoryUrl="$2"

    echo "${repositoryUrl}/contents/${relativeFilePath}"
}

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

    local responseBody="$(curl -H "${GITHUB_REQUEST_HEADER}" \
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

    local masterBranchData="$(curl -H "${GITHUB_REQUEST_HEADER}" \
        "${repositoryRefsUrl}/heads/${defaultBranchName}")"

    local lastCommitSha="$(read_json_field 'sha' "${masterBranchData}")"

    local requestData="{\"ref\":\"refs/heads/${branchName}\",
                        \"sha\":${lastCommitSha}}"

    curl -H "${GITHUB_REQUEST_HEADER}" \
        -H "Content-Type: application/json" \
        --data "${requestData}" \
        "${repositoryRefsUrl}"
}

delete_branch() {
    local branchName="$1"
    local repositoryUrl="$2"

    curl -H "${GITHUB_REQUEST_HEADER}" \
        -X DELETE \
        "${repositoryUrl}/git/refs/heads/${branchName}"
}

get_status_check_result() {
    local branchName="$1"
    local repositoryUrl="$2"

    local statusData="$(curl -H "${GITHUB_REQUEST_HEADER}" \
        "${repositoryUrl}/commits/heads/${branchName}/status")"

    local statusCheckResult="$(read_json_field 'state' "${statusData}")"

    # Remove all quotes from the result.
    statusCheckResult="${statusCheckResult//\"/}"

    echo "${statusCheckResult}"
}

get_file_data() {
    local fileUrl="$1"
    echo "$(curl -H "${GITHUB_REQUEST_HEADER}" "${fileUrl}")"
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

    curl -H "${GITHUB_REQUEST_HEADER}" \
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

    local creationResponse="$(curl -H "${GITHUB_REQUEST_HEADER}" \
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

    curl -H "${GITHUB_REQUEST_HEADER}" \
        -H "Content-Type: application/json" \
        -X PATCH \
        --data "${requestData}" \
        "${repositoryUrl}/issues/${pullRequestNumber}"
}

merge_pull_request() {
    local pullRequestNumber="$1"
    local repositoryUrl="$2"

    local httpStatusLabel='HTTP_STATUS:'

    local mergeResponse="$(curl -H "${GITHUB_REQUEST_HEADER}" \
        -H "Content-Type: application/json" \
        -X PUT \
        --write-out "${httpStatusLabel}%{http_code}" \
        "${repositoryUrl}/pulls/${pullRequestNumber}/merge")"

    local mergeRequestStatusCode="$(retrieve_labeled_value "${httpStatusLabel}" \
        "${mergeResponse}")"

    echo "$(check_status_code "${mergeRequestStatusCode}" '200')"
}

obtain_version() {
    local repositoryUrl="$1"
    local fileWithVersion="$2"
    local versionLabel="$3"

    local fullFilePath="$(obtain_full_file_path "${fileWithVersion}" \
        "${repositoryUrl}")"

    local fileData="$(get_file_data ${fullFilePath})"
    local fileContent="$(read_file_content "${fileData}")"
    local fileContentDecoded="$(decode "${fileContent}")"

    local stringWithVersion="$(search_for_string_assignment "${versionLabel}" \
        "${fileContentDecoded}")"
    local version="$(remove_label "${versionLabel}" "${stringWithVersion}")"

    echo "${version}"
}

update_version() {
    local targetVersion="$1"

    local repositoryUrl="$2"
    local fileWithVersion="$3"
    local versionLabel="$4"

    local newBranchName="$5"
    local branchToMergeInto="$6"

    local commitMessage="$7"
    local pullRequestTitle="$8"
    local pullRequestBody="$9"
    local pullRequestAssignee="${10}"

    local statusCheckTimeoutSeconds="${11}"

    local fullFilePath="$(obtain_full_file_path "${fileWithVersion}" \
        "${repositoryUrl}")"

    local branchExists="$(check_branch_exists "${newBranchName}" "${repositoryUrl}")"

    if [ "${branchExists}" = 'true' ]; then
        fullFilePath="${fullFilePath}?ref=${newBranchName}"
    fi

    local fileData="$(get_file_data ${fullFilePath})"
    local fileContent="$(read_file_content "${fileData}")"
    local fileContentDecoded="$(decode "${fileContent}")"

    local stringWithVersion="$(search_for_string_assignment "${versionLabel}" \
        "${fileContentDecoded}")"

    local version="$(remove_label "${versionLabel}" "${stringWithVersion}")"

    if [ "${targetVersion}" != "${version}" ]; then
        if [ "${branchExists}" = 'false' ]; then
            create_branch "${newBranchName}" \
                "${branchToMergeInto}" \
                "${repositoryUrl}"
        fi

        local labelString="$(remove_version "${version}" "${stringWithVersion}")"
        local newText="$(substitute_version "${version}" \
            "${targetVersion}" \
            "${labelString}" \
            "${fileContentDecoded}")"

        local encodedNewText="$(encode "${newText}")"
        local fileSha="$(obtain_file_sha "${fileData}")"
        commit_file "${commitMessage}" \
            "${fullFilePath}" \
            "${encodedNewText}" \
            "${fileSha}" \
            "${newBranchName}"

        # Wait so Travis pull request build doesn't auto-cancel Travis push build.
        # This leads to status check being failed and PR not being able to merge.
        sleep 10

        if [ "$branchExists" = false ]; then
            local pullRequestNumber="$(create_pull_request "${pullRequestTitle}" \
                "${newBranchName}" \
                "${branchToMergeInto}" \
                "${pullRequestBody}" \
                "${repositoryUrl}")"

            local statusCheckResult='pending'

            echo 'Requesting status checks result...'
            local secondsElapsed=0

            # Emulate do...while loop.
            while true; do
                statusCheckResult="$(get_status_check_result \
                    "${newBranchName}" \
                    "${repositoryUrl}")"

                [ "${statusCheckResult}" = 'pending' ] && \
                    [ ${secondsElapsed} -lt ${statusCheckTimeoutSeconds} ] \
                    || break

                # Wait before the next request.
                local secondsToWait=10
                sleep ${secondsToWait}

                # Use arithmetic context for integer addition.
                secondsElapsed=$((${secondsElapsed} + 10))

                echo "Time elapsed: ${secondsElapsed}s"
            done

            echo "Status checks result: ${statusCheckResult}"

            if [ "${statusCheckResult}" = 'success' ]; then
                local mergeSuccessful="$(merge_pull_request "${pullRequestNumber}" \
                "${repositoryUrl}")"
                if [ "${mergeSuccessful}" = 'true' ]; then
                    delete_branch "${newBranchName}" "${repositoryUrl}"
                    return 0
                fi
            fi

            # If status checks indicated failure or merge was unsuccessful assign the PR to the assignee.
            assign_pull_request "${pullRequestNumber}" \
                        "${repositoryUrl}" \
                        "${pullRequestAssignee}"
        fi
    fi
}

main() {
    local githubToken="$1"

    local sourceRepository="$2"
    local sourceFileWithVersion="$3"
    local sourceVersionLabel="$4"

    local targetRepository="$5"
    local targetFileWithVersion="$6"
    local targetVersionLabel="$7"

    local newBranchName="$8"
    local branchToMergeInto="$9"

    local commitMessage="${10}"
    local pullRequestTitle="${11}"
    local pullRequestBody="${12}"
    local pullRequestAssignee="${13}"

    local statusCheckTimeoutSeconds="${14}"

    GITHUB_REQUEST_HEADER="Authorization: token ${githubToken}"

    local targetVersion="$(obtain_version "${sourceRepository}" \
        "${sourceFileWithVersion}" \
        "${sourceVersionLabel}")"

    update_version "${targetVersion}" \
        "${targetRepository}" \
        "${targetFileWithVersion}" \
        "${targetVersionLabel}" \
        "${newBranchName}" \
        "${branchToMergeInto}" \
        "${commitMessage}" \
        "${pullRequestTitle}" \
        "${pullRequestBody}" \
        "${pullRequestAssignee}" \
        "${statusCheckTimeoutSeconds}"
}

main "$@"

#!/bin/bash
#
# Retrieve the current version of the source library and update the target library dependency version with it.
#
# In this script 'echo' to stdout and command substitution on call are used to emulate the function return value.

# Header used for GitHub API request authorization.
# Its content is updated with an authorization token received from the command line arguments on script execution.
GIT_AUTHORIZATION_HEADER=''

#######################################
# Connect the repository URL and the relative file path to get the full path to the repository file.
#
# Arguments:
#   relativeFilePath - file path relative to repository root
#   repositoryUrl - URL of the repository via GitHub API
#
# Returns:
#   Full path to the file via GitHub API.
#######################################
function obtainFullFilePath() {
    local relativeFilePath="$1"
    local repositoryUrl="$2"

    echo "${repositoryUrl}/contents/${relativeFilePath}"
}

#######################################
# Search for strings containing the specified substring in the text.
#
# Arguments:
#   string - substring to be searched for
#   text - text, i.e. string with line breaks, in which to perform the search
#
# Returns:
#   All strings of the text containing the specified substring.
#######################################
function searchForString() {
    local string="$1"
    local text="$2"

    echo "$(grep "${string}" <<< "${text}")"
}

#######################################
# Search for strings containing specified substring assignment in text.
#
# This function searches for the pattern "*substring* = ..." with an arbitrary amount of spaces.
# It can be used to search for the variable assignment in the code.
#
# Arguments:
#   string - substring whose assignment is searched
#   text - text, i.e. string with line breaks, in which to perform the search
#
# Returns:
#   All strings of the text that contain the specified substring assignment.
#######################################
function searchForValueAssignment() {
    local string="$1"
    local text="$2"

    local patternToSearch="${string} *= *"

    echo "$(searchForString "${patternToSearch}" "${text}")"
}

#######################################
# Acquire JSON field value from the JSON data.
#
# This function searches for the pattern "field": "value".
# This function is not suitable for the numeric JSON values as they have no quotes around them.
#
# Arguments:
#   fieldName - name of the JSON field
#   jsonData - JSON data to search through
#
# Returns:
#   Field value or an empty string if the field is not found in the data or has the wrong format.
#######################################
function readJsonField() {
    local fieldName="$1"
    local jsonData="$2"

    echo "$(grep -o -m 1 '"'${fieldName}'": *"[^"]*"' <<< "${jsonData}" \
        | grep -o '"[^"]*"$')"
}

#######################################
# Acquire numeric JSON field value from the JSON data.
#
# Numeric fields in JSON don't have quotes around their value so they need a separate searching pattern.
#
# Arguments:
#   fieldName - name of the numeric field
#   jsonData - JSON data to search through
#
# Returns:
#   Field value or an empty string if the field is not found in the data or has the wrong format.
function readJsonFieldNumeric() {
    local fieldName="$1"
    local jsonData="$2"

    echo "$(grep -o '"'${fieldName}'": *[0-9]*' <<< "${jsonData}" \
        | grep -o '[0-9]*$')"
}

#######################################
# Obtain 'content' field from the file JSON data and strip it from the line breaks and quotes.
#
# Function is primarily used for the JSON data returned by the file acquisition GitHub API.
# Content is supposed to be encoded so line breaks and quotes can spoil the decoding process.
#
# Arguments:
#   fileData - file JSON data
#
# Returns:
#   File's content, which is most probably encoded.
function obtainFileContent() {
    local fileData="$1"

    local fileContent="$(readJsonField 'content' "${fileData}")"
    fileContent="${fileContent//\\n/}"
    fileContent="${fileContent//\"/}"

    echo "${fileContent}"
}

#######################################
# Obtain 'sha' field value from the file JSON data.
#
# Function is primarily used for the JSON data returned by the file acquisition GitHub API.
#
# Arguments:
#   fileData - file JSON data
#
# Returns:
#   File's sha data.
function obtainFileSha() {
    local fileData="$1"

    echo "$(readJsonField 'sha' "${fileData}")"
}

#######################################
# Obtain value preceded by the exact label in the text.
#
# Can be useful with the commands like "curl --write-out" where it is known exactly what precedes the searched value.
#
# Arguments:
#   label - exact string preceding the searched value
#   text - text to search through
#
# Returns:
#   Value preceded by the specified label.
function retrieveLabeledValue() {
    local label="$1"
    local text="$2"

    echo "$(tr -d '\n' <<< "${text}" | sed -e 's/.*'"${label}"'//')"
}

#######################################
# Check if the status code matches the desired value.
#
# Arguments:
#   code - status code to check
#   expectedValue - value to check against
#
# Returns:
#   'true' if the code matches the value and 'false' otherwise.
function checkStatusCode() {
    local code="$1"
    local expectedValue="$2"

    if [ "${code}" = "${expectedValue}" ]; then
        echo 'true'
    else
        echo 'false'
    fi
}

#######################################
# Base64-encode the specified text.
#
# This function doesn't line wrap the encoded content.
#
# Arguments:
#   text - text, i.e. string with line breaks, to be encoded
#
# Returns:
#   Encoded text.
function encode() {
    local text="$1"

    echo "$(base64 --wrap=0 <<< "${text}")"
}

#######################################
# Base64-decode the specified text.
#
# Arguments:
#   text - text to be decoded
#
# Returns:
#   Decoded text or an error message if it can't be decoded.
function decode() {
    local text="$1"

    echo "$(base64 -d <<< "${text}")"
}

#######################################
# Retrieve the value assigned to the label in the string of kind "*label* = *value*".
#
# This function is primarily used to obtain the value assigned to some variable in the code.
#
# Arguments:
#   label - substring to which the value is assigned
#   string - string with value assignment
#
# Returns:
#   Value assigned to the label.
function retrieveAssignedValue() {
    local label="$1"
    local string="$2"

    labelString="$(grep -o "${label}"'[ =]*' <<< "${string}")"
    local patternToRemove="*${labelString}"

    echo "${string##${patternToRemove}}"
}

#######################################
# Remove the assigned value from the string of kind "*label* = *value*".
#
# This is helper function to retrieve the desired substring of kind "*label* = " to later assign a new value to it.
#
# Arguments:
#   value - value assigned
#   string - string with value assignment
#
# Returns:
#   String without the value assigned.
function removeAssignedValue() {
    local value="$1"
    local stringWithVersion="$2"

    local versionSubstringStart="$(grep -b -o "$value" <<< "$stringWithVersion" \
                                    | cut -d: -f1)"

    echo "${stringWithVersion:0:${versionSubstringStart}}"
}

#######################################
# Substitute the value assigned to some label in text.
#
# Arguments:
#   oldValue - value to substitute
#   newValue - value to assign to the label
#   labelString - string in form "*label* = "
#   text - text in which the value assigned is replaced
#
# Returns:
#   New text where the label is assigned the new value.
function substituteValue() {
    local oldValue="$1"
    local newValue="$2"
    local labelString="$3"
    local text="$4"

    local oldAssignmentString="${labelString}${oldValue}"
    local newAssignmentString="${labelString}${newValue}"
    local newText="$(sed "s/${oldAssignmentString}/${newAssignmentString}/g" \
        <<< "${text}")"

    echo "${newText}"
}

#######################################
# Check if a branch with the specified name exists in the git repository.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   branchName - name of the branch, without "heads" prefix
#   repositoryUrl - URL of the repository via GitHub API
#
# Returns:
#   'true' if the branch exists and 'false' otherwise.
function checkBranchExists() {
    local branchName="$1"
    local repositoryUrl="$2"

    local httpStatusLabel='HTTP_STATUS:'

    local responseBody="$(curl -H "${GIT_AUTHORIZATION_HEADER}" \
        --write-out "${httpStatusLabel}%{http_code}" \
        "${repositoryUrl}/git/refs/heads/${branchName}")"

    local branchRequestStatusCode="$(retrieveLabeledValue "${httpStatusLabel}" \
        "${responseBody}")"

    echo "$(checkStatusCode "${branchRequestStatusCode}" '200')"
}

#######################################
# Create a branch with the specified name in the git repository.
#
# If the branch already exists, this function does nothing.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   branchName - name of the branch, without "heads" prefix
#   baseBranchName - name of the branch to derive from, without "heads" prefix
#   repositoryUrl - URL of the repository via GitHub API
#
# Returns:
#   None.
function createBranch() {
    local branchName="$1"
    local baseBranchName="$2"
    local repositoryUrl="$3"

    local repositoryRefsUrl="${repositoryUrl}/git/refs"

    local baseBranchData="$(curl -H "${GIT_AUTHORIZATION_HEADER}" \
        "${repositoryRefsUrl}/heads/${baseBranchName}")"

    local lastCommitSha="$(readJsonField 'sha' "${baseBranchData}")"

    local requestData="{\"ref\":\"refs/heads/${branchName}\",
                        \"sha\":${lastCommitSha}}"

    curl -H "${GIT_AUTHORIZATION_HEADER}" \
        -H "Content-Type: application/json" \
        --data "${requestData}" \
        "${repositoryRefsUrl}"
}

#######################################
# Delete a branch with the specified name from the git repository.
#
# If the branch does not exist, this function does nothing.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   branchName - name of the branch, without "heads" prefix
#   repositoryUrl - URL of the repository via GitHub API
#
# Returns:
#   None.
function deleteBranch() {
    local branchName="$1"
    local repositoryUrl="$2"

    curl -H "${GIT_AUTHORIZATION_HEADER}" \
        -X DELETE \
        "${repositoryUrl}/git/refs/heads/${branchName}"
}

#######################################
# Get combined status check result for the branch.
#
# This function is primarily used for the branches that have pull request open to know if it can be already merged.
#
# The combined status check result can be: 'pending', 'success' or 'failure'.
# The 'failure' status means that one or more of status checks have already failed.
# The 'success' status means that all status checks have ended with the 'success' status.
# The 'pending' status means that some checks are still calculated and none of them have failed yet.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   branchName - name of the branch, without "heads" prefix
#   repositoryUrl - URL of the repository via GitHub API
#
# Returns:
#   Combined result of status checks for the specified branch or nothing if the branch is not found.
function getStatusCheckResult() {
    local branchName="$1"
    local repositoryUrl="$2"

    local statusData="$(curl -H "${GIT_AUTHORIZATION_HEADER}" \
        "${repositoryUrl}/commits/heads/${branchName}/status")"

    local statusCheckResult="$(readJsonField 'state' "${statusData}")"

    # Remove all quotes from the result.
    statusCheckResult="${statusCheckResult//\"/}"

    echo "${statusCheckResult}"
}

#######################################
# Request the file data from the git repository.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   fileUrl - full path to the file via GitHub API
#
# Returns:
#   File JSON data or an error if the file is not found.
function getFileData() {
    local fileUrl="$1"

    echo "$(curl -H "${GIT_AUTHORIZATION_HEADER}" "${fileUrl}")"
}

#######################################
# Commit and push the file to the git repository.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   commitMessage - title for the commit
#   fileUrl - full path to the file via GitHub API
#   newFileContents - updated file contents, base64-encoded
#   fileSha - sha data of the file
#   branch - branch to push to
#
# Returns:
#   None.
function commitAndPushFile() {
    local commitMessage="$1"
    local fileUrl="$2"
    local newFileContents="$3"
    local fileSha="$4"
    local branch="$5"

    local requestData="{\"message\":\"${commitMessage}\",
                        \"content\":\"${newFileContents}\",
                        \"sha\":${fileSha},
                        \"branch\":\"${branch}\"}"

    curl -H "${GIT_AUTHORIZATION_HEADER}" \
        -H "Content-Type: application/json" \
        -X PUT \
        --data "${requestData}" \
        "${fileUrl}"
}

#######################################
# Create a pull request to the git repository.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   pullRequestName - title for the pull request
#   head - branch to be merged
#   base - branch to merge to
#   body - text of the pull request body
#   repositoryUrl - URL of the repository via GitHub API
#
# Returns:
#   Number of the newly created pull request or nothing if its creation failed.
function createPullRequest() {
    local pullRequestName="$1"
    local head="$2"
    local base="$3"
    local body="$4"
    local repositoryUrl="$5"

    local requestData="{\"title\":\"${pullRequestName}\",
                        \"head\":\"${head}\",
                        \"base\":\"${base}\",
                        \"body\":\"${body}\"}"

    local creationResponse="$(curl -H "${GIT_AUTHORIZATION_HEADER}" \
        -H "Content-Type: application/json" \
        --data "$requestData" \
        "${repositoryUrl}/pulls")"

    local pullRequestNumber="$(readJsonFieldNumeric \
        'number' \
        "${creationResponse}")"

    echo "${pullRequestNumber}"
}

#######################################
# Assign pull request in the git repository.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   pullRequestNumber - number of the pull request
#   repositoryUrl - URL of the repository via GitHub API
#   assignee - login of the github user to assign pull request to
#
# Returns:
#   None.
function assignPullRequest() {
    local pullRequestNumber="$1"
    local repositoryUrl="$2"
    local assignee="$3"

    local requestData="{\"assignees\":[\"${assignee}\"]}"

    curl -H "${GIT_AUTHORIZATION_HEADER}" \
        -H "Content-Type: application/json" \
        -X PATCH \
        --data "${requestData}" \
        "${repositoryUrl}/issues/${pullRequestNumber}"
}

#######################################
# Merge pull request in the git repository.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   pullRequestNumber - number of the pull request
#   repositoryUrl - URL of the repository via GitHub API
#
# Returns:
#   'true' if the merge was successful and 'false' otherwise.
function mergePullRequest() {
    local pullRequestNumber="$1"
    local repositoryUrl="$2"

    local httpStatusLabel='HTTP_STATUS:'

    local mergeResponse="$(curl -H "${GIT_AUTHORIZATION_HEADER}" \
        -H "Content-Type: application/json" \
        -X PUT \
        --write-out "${httpStatusLabel}%{http_code}" \
        "${repositoryUrl}/pulls/${pullRequestNumber}/merge")"

    local mergeRequestStatusCode="$(retrieveLabeledValue "${httpStatusLabel}" \
        "${mergeResponse}")"

    echo "$(checkStatusCode "${mergeRequestStatusCode}" '200')"
}

#######################################
# Obtain the version of the specified library from the git repository.
#
# This function searches for the value assignment in the specified file of the git repository.
# From the string of the type "*versionVariable* = *value*" it returns the 'value' to the caller.
# This way it is possible to search for the specific library version in the Gradle configuration files.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   repositoryUrl - URL of the repository via GitHub API
#   fileWithVersion - relative path to the configuration file containing version assignment
#   versionVariable - name of the variable representing the version of the library
#
# Returns:
#   Value assigned to the specified version variable.
function obtainVersion() {
    local repositoryUrl="$1"
    local fileWithVersion="$2"
    local versionVariable="$3"

    local fullFilePath="$(obtainFullFilePath "${fileWithVersion}" \
        "${repositoryUrl}")"

    local fileData="$(getFileData ${fullFilePath})"
    local fileContent="$(obtainFileContent "${fileData}")"
    local fileContentDecoded="$(decode "${fileContent}")"

    local stringWithVersion="$(searchForValueAssignment "${versionVariable}" \
        "${fileContentDecoded}")"
    local version="$(retrieveAssignedValue "${versionVariable}" \
        "${stringWithVersion}")"

    echo "${version}"
}

#######################################
# Update version of the specified library in the git repository.
#
# This function searches for the value assignment in the specified file of the git repository.
# For the string of the type "*versionVariable* = *value*" it replaces the 'value' with the specified 'targetVersion'.
#
# After the new value is assigned to the version variable, the file is committed to a new branch of the repository.
# A pull request is then opened for the new branch and, if all status checks are successful, immediately merged.
# If the status checks indicate a failure, the pull request is assigned to the specified github user.
#
# If the branch and pull request already exist, the function just updates the version there if necessary.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   targetVersion - new version to be set for the specified library
#   repositoryUrl - URL of the repository via GitHub API
#   fileWithVersion - relative path to the configuration file containing version assignment
#   versionVariable - name of the variable representing the version of the library of interest
#   newBranchName - name of the branch for the updated file
#   branchToMergeInto - name of the branch to merge the updated version of the file into
#   commitMessage - title of the commit with the updated version of the file
#   pullRequestTitle - title of the pull request for the new branch
#   pullRequestBody - body of the pull request for the new branch
#   pullRequestAssignee - default assignee of the pull request if it can't be merged immediately
#   statusCheckTimeoutSeconds - how long to wait for 'pending' PR status checks before accounting them as 'failure'
#
# Returns:
#   None.
function updateVersion() {
    local targetVersion="$1"
    local repositoryUrl="$2"
    local fileWithVersion="$3"
    local versionVariable="$4"
    local newBranchName="$5"
    local branchToMergeInto="$6"
    local commitMessage="$7"
    local pullRequestTitle="$8"
    local pullRequestBody="$9"
    local pullRequestAssignee="${10}"
    local statusCheckTimeoutSeconds="${11}"

    local fullFilePath="$(obtainFullFilePath "${fileWithVersion}" \
        "${repositoryUrl}")"

    local branchExists="$(checkBranchExists "${newBranchName}" \
        "${repositoryUrl}")"

    if [ "${branchExists}" = 'true' ]; then

        # If branch already exists take the file from there.
        fullFilePath="${fullFilePath}?ref=${newBranchName}"
    fi

    local fileData="$(getFileData ${fullFilePath})"
    local fileContent="$(obtainFileContent "${fileData}")"
    local fileContentDecoded="$(decode "${fileContent}")"

    local stringWithVersion="$(searchForValueAssignment "${versionVariable}" \
        "${fileContentDecoded}")"

    local version="$(retrieveAssignedValue "${versionVariable}" \
        "${stringWithVersion}")"

    if [ "${targetVersion}" != "${version}" ]; then
        if [ "${branchExists}" = 'false' ]; then
            createBranch "${newBranchName}" \
                "${branchToMergeInto}" \
                "${repositoryUrl}"
        fi

        local labelString="$(removeAssignedValue "${version}" \
            "${stringWithVersion}")"
        local newText="$(substituteValue "${version}" \
            "${targetVersion}" \
            "${labelString}" \
            "${fileContentDecoded}")"

        local encodedNewText="$(encode "${newText}")"
        local fileSha="$(obtainFileSha "${fileData}")"
        commitAndPushFile "${commitMessage}" \
            "${fullFilePath}" \
            "${encodedNewText}" \
            "${fileSha}" \
            "${newBranchName}"

        if [ "${branchExists}" = 'false' ]; then

            # Wait so Travis pull request build doesn't auto-cancel Travis push build.
            # This leads to the push build status check being failed and PR not being able to merge.
            sleep 60

            local pullRequestNumber="$(createPullRequest "${pullRequestTitle}" \
                "${newBranchName}" \
                "${branchToMergeInto}" \
                "${pullRequestBody}" \
                "${repositoryUrl}")"

            local statusCheckResult='pending'

            echo 'Requesting status checks result...'

            local secondsElapsed=0

            # Emulate do...while loop.
            while true; do
                statusCheckResult="$(getStatusCheckResult \
                    "${newBranchName}" \
                    "${repositoryUrl}")"

                [ "${statusCheckResult}" = 'pending' ] && \
                    [ ${secondsElapsed} -lt ${statusCheckTimeoutSeconds} ] \
                    || break

                # Wait before the next request.
                local secondsToWait=10
                sleep ${secondsToWait}

                secondsElapsed=$((${secondsElapsed} + ${secondsToWait}))

                echo "Time elapsed: ${secondsElapsed}s"
            done

            echo "Status checks result: ${statusCheckResult}"

            if [ "${statusCheckResult}" = 'success' ]; then
                local mergeSuccessful="$(mergePullRequest "${pullRequestNumber}" \
                    "${repositoryUrl}")"
                if [ "${mergeSuccessful}" = 'true' ]; then
                    deleteBranch "${newBranchName}" "${repositoryUrl}"
                    return 0
                fi
            fi

            # If status checks indicated failure or merge was unsuccessful, assign the PR to the assignee.
            assignPullRequest "${pullRequestNumber}" \
                "${repositoryUrl}" \
                "${pullRequestAssignee}"
        fi
    fi
}

#######################################
# Retrieve the current version of the source library and update target library dependency version with it.
#
# Globals:
#   GIT_AUTHORIZATION_HEADER
#
# Arguments:
#   githubToken - secret token for the GitHub authorization
#   sourceRepository - source repository URL via GitHub API
#   sourceFileWithVersion - relative path to the configuration file containing source version
#   sourceVersionVariable - name of the variable representing the source version of the library of interest
#   targetRepository - target repository URL via GitHub API
#   targetFileWithVersion - relative path to the configuration file containing target version
#   targetVersionVariable - name of the variable representing the target version of the library of interest
#   newBranchName - name of the branch for the updated file
#   branchToMergeInto - name of the branch to merge the updated version of the file into
#   commitMessage - title of the commit with the updated version of the file
#   pullRequestTitle - title of the pull request for the new branch
#   pullRequestBody - body of the pull request for the new branch
#   pullRequestAssignee - default assignee of the pull request if it can't be merged immediately
#   statusCheckTimeoutSeconds - how long to wait for 'pending' PR status checks before accounting them as 'failure'
#
# Returns:
#   None.
#
# See:
#   obtainVersion(), updateVersion() for the implementation details.
function main() {
    local gitAuthorizationToken="$1"

    local sourceRepository="$2"
    local sourceFileWithVersion="$3"
    local sourceVersionVariable="$4"

    local targetRepository="$5"
    local targetFileWithVersion="$6"
    local targetVersionVariable="$7"

    local newBranchName="$8"
    local branchToMergeInto="$9"
    local commitMessage="${10}"
    local pullRequestTitle="${11}"
    local pullRequestBody="${12}"
    local pullRequestAssignee="${13}"
    local statusCheckTimeoutSeconds="${14}"

    GIT_AUTHORIZATION_HEADER="Authorization: token ${gitAuthorizationToken}"

    local targetVersion="$(obtainVersion "${sourceRepository}" \
        "${sourceFileWithVersion}" \
        "${sourceVersionVariable}")"

    updateVersion "${targetVersion}" \
        "${targetRepository}" \
        "${targetFileWithVersion}" \
        "${targetVersionVariable}" \
        "${newBranchName}" \
        "${branchToMergeInto}" \
        "${commitMessage}" \
        "${pullRequestTitle}" \
        "${pullRequestBody}" \
        "${pullRequestAssignee}" \
        "${statusCheckTimeoutSeconds}"
}

main "$@"

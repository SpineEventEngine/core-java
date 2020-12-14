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
# Retrieve the current version of the source library and update the target library dependency version with it.
#
# In this script 'echo' to stdout and command substitution on call are used to emulate the function return value.
#
# The "main()" function is the entry point of this script.
#
# In general, it performs the following steps:
#
# 1. Read the value of the specified variable in the specified file in the "source" repository.
#    This value is supposed to represent the current version of the library.
# 2. Read the value of the specified variable in the "target" repository.
#    It is supposed to represent the dependency version of the "target" repository.
# 3. Compare "source" and "target" versions.
# 4. If versions are equal, quit immediately. Otherwise go to the step 5.
# 5. Retrieve the "target" file and replace the "target" version in it with the "source" version.
# 6. Commit and push the file to the new branch with the specified name.
# 7. Open the pull request from the new branch to the specified default one (usually "master").
# 8. Assign the pull request to the specified GitHub user.
#
# If the version update is triggered when the specified branch already exists (for example, from
# the previous version update), the script just updates the dependency version directly in this
# branch.
#
# For the detailed description of the command line arguments accepted by this script, see "main()" function.

# Header used for the GitHub API request authorization.
# Its content is updated with an authorization token received from the command line arguments on the script execution.
GIT_AUTHORIZATION_HEADER=''

#######################################
# Log the message into the stdout.
#
# The usual 'echo' command doesn't allow normal indentation for the line-wrapped log strings.
# This function squeezes all the spaces in the message, allowing it to have the same indentation as the rest of the code.
#
# The last-line 'echo' of this function is not supposed to be used as a return value.
# Instead it's used to write the message directly to the stdout.
#
# Arguments:
#   message - message to output
#
# Returns:
#   None.
#######################################
function log() {
    local message="$1"

    local messageToShow="$(tr -s ' ' <<< "${message}")"

    echo "${messageToShow}"
}

#######################################
# Retrieve the repository name from the repository URL.
#
# This helper function removes GitHub API "repos" prefix from the repository URL.
# For example, for the "https://api.github.com/repos/BVLC/caffe" repository URL it will return "BVLC/caffe".
#
# Function's primary usage is to enhance the debug output.
#
# Arguments:
#   repositoryUrl - URL of the repository via GitHub API
#
# Returns:
#   The name of the repository.
#######################################
function retrieveRepositoryName() {
    local repositoryUrl="$1"

    local gitReposAddress='https://api.github.com/repos/'

    echo "$(replace "${gitReposAddress}" "" "${repositoryUrl}")"
}

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
# Search for strings containing specified substring assignment in the text.
#
# This function searches for the pattern "*substring* = ..." with an arbitrary amount of spaces.
# The primary usage of the function is to search for the variable assignment in the code.
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
# Replace some pattern in the text by another substring.
#
# Arguments:
#   pattern - pattern or substring to replace
#   replacement - string with which to replace the pattern
#   text - text, i.e. string with or without line breaks, in which to perform the replacement
#
# Returns:
#   Text with the *pattern* replaced by *replacement*.
#######################################
function replace() {
    local pattern="$1"
    local replacement="$2"
    local text="$3"

    echo "${text//${pattern}/${replacement}}"
}

#######################################
# Acquire JSON field string value from the JSON data.
#
# This function searches for the pattern "field": "value".
# This function is not suitable for the numeric or other JSON values as they have no quotes around them.
#
# Arguments:
#   fieldName - name of the JSON field
#   jsonData - JSON data to search through
#
# Returns:
#   Field value or an empty string if the field is not found in the data or has the wrong format.
#######################################
function readJsonFieldString() {
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
# The function is primarily used for the JSON data returned by the file acquisition GitHub API.
# Content is supposed to be encoded so line breaks and quotes can spoil the decoding process.
#
# Arguments:
#   fileData - file JSON data
#
# Returns:
#   File's content.
function obtainFileContent() {
    local fileData="$1"

    local fileContent="$(readJsonFieldString 'content' "${fileData}")"
    fileContent="$(replace "\\\n" "" "${fileContent}")"
    fileContent="$(replace "\"" "" "${fileContent}")"

    echo "${fileContent}"
}

#######################################
# Obtain 'sha' field value from the file JSON data.
#
# The function is primarily used for the JSON data returned by the file acquisition GitHub API.
#
# Arguments:
#   fileData - file JSON data
#
# Returns:
#   File's sha data.
function obtainFileSha() {
    local fileData="$1"

    echo "$(readJsonFieldString 'sha' "${fileData}")"
}

#######################################
# Obtain value preceded by the exact label in the text.
#
# The function can be useful when it is known exactly what precedes the searched value in the text.
# For example, with the commands like "curl --write-out".
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
# The number of spaces between the label/value and the equality sign doesn't make a difference.
#
# Arguments:
#   label - substring to which the value is assigned
#   string - string with the value assignment
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
# This is a helper function to retrieve the desired substring of kind "*label* = " to later assign a new value to it.
#
# The number of spaces between the label/value and the equality sign doesn't make a difference.
#
# Arguments:
#   value - value assigned
#   string - string with the value assignment
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
# Substitute the value assigned to some label in the text.
#
# Arguments:
#   oldValue - value to substitute
#   newValue - value to assign to the label
#   labelString - string in the form "*label* = " with an arbitrary amount of spaces before and after "="
#   text - text in which the value assigned is replaced
#
# Returns:
#   New text where the new value is assigned to the label.
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

    local responseBody="$(curl -s -H "${GIT_AUTHORIZATION_HEADER}" \
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
#   branchName - name of the new branch, without "heads" prefix
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

    local baseBranchData="$(curl -s -H "${GIT_AUTHORIZATION_HEADER}" \
        "${repositoryRefsUrl}/heads/${baseBranchName}")"

    local lastCommitSha="$(readJsonFieldString 'sha' "${baseBranchData}")"

    local requestData="{\"ref\":\"refs/heads/${branchName}\",
                        \"sha\":${lastCommitSha}}"

    curl -s -H "${GIT_AUTHORIZATION_HEADER}" \
        -H "Content-Type: application/json" \
        --data "${requestData}" \
        "${repositoryRefsUrl}" > /dev/null
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

    echo "$(curl -s -H "${GIT_AUTHORIZATION_HEADER}" "${fileUrl}")"
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

    curl -s -H "${GIT_AUTHORIZATION_HEADER}" \
        -H "Content-Type: application/json" \
        -X PUT \
        --data "${requestData}" \
        "${fileUrl}" > /dev/null
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
#   base - branch to merge into
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

    local creationResponse="$(curl -s -H "${GIT_AUTHORIZATION_HEADER}" \
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

    curl -s -H "${GIT_AUTHORIZATION_HEADER}" \
        -H "Content-Type: application/json" \
        -X PATCH \
        --data "${requestData}" \
        "${repositoryUrl}/issues/${pullRequestNumber}" > /dev/null
}

#######################################
# Obtain the version of the specified library from the git repository.
#
# This function searches for the value assignment in the specified file of the git repository.
# From the string of the type "*versionVariable* = *value*" it returns the 'value' to the caller.
# The number of spaces between the variable/value and the equality sign doesn't make a difference.
#
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
# The number of spaces between the variable/value and the equality sign doesn't make a difference.
#
# After the new value is assigned to the version variable, the file is committed to a new branch of the repository.
# A pull request is then opened for the new branch and assigned to the specified *pullRequestAssignee*.
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
#   versionVariable - name of the variable representing the version of the library
#   newBranchName - name of the branch for the updated file
#   branchToMergeInto - name of the branch to merge the updated version of the file into
#   commitMessage - title of the commit with the updated version of the file
#   pullRequestTitle - title of the pull request for the new branch
#   pullRequestBody - body of the pull request for the new branch
#   pullRequestAssignee - assignee of the created pull request
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


    local repositoryName="$(retrieveRepositoryName "${repositoryUrl}")"

    log "Running version update for the dependency ${versionVariable} \
        in the file ${fileWithVersion} in the repository ${repositoryName}."

    local fullFilePath="$(obtainFullFilePath "${fileWithVersion}" \
        "${repositoryUrl}")"

    local targetVersionNoQuotes="$(replace "\'" "" "${targetVersion}")"

    newBranchName="$(replace "\*version\*" \
        "${targetVersionNoQuotes}" \
        "${newBranchName}")"

    local branchExists="$(checkBranchExists "${newBranchName}" \
        "${repositoryUrl}")"

    log "Branch with the name ${newBranchName} already exists in ${repositoryName}: \
        ${branchExists}."

    if [ "${branchExists}" = 'true' ]; then
        log "The branch already exists, checking if it's still necessary to update \
            the file there."

        # If the branch already exists, take the file from there.
        fullFilePath="${fullFilePath}?ref=${newBranchName}"
    fi

    local fileData="$(getFileData ${fullFilePath})"
    local fileContent="$(obtainFileContent "${fileData}")"
    local fileContentDecoded="$(decode "${fileContent}")"

    local stringWithVersion="$(searchForValueAssignment "${versionVariable}" \
        "${fileContentDecoded}")"

    local version="$(retrieveAssignedValue "${versionVariable}" \
        "${stringWithVersion}")"

    log "The actual version of the library is ${targetVersion}, and the dependency \
        version is ${version}."

    if [ "${targetVersion}" != "${version}" ]; then
        if [ "${branchExists}" = 'false' ]; then
            createBranch "${newBranchName}" \
                "${branchToMergeInto}" \
                "${repositoryUrl}"
            log "Created branch ${newBranchName} derived from ${branchToMergeInto} \
                in ${repositoryName}."
        fi

        local labelString="$(removeAssignedValue "${version}" \
            "${stringWithVersion}")"
        local newFileContent="$(substituteValue "${version}" \
            "${targetVersion}" \
            "${labelString}" \
            "${fileContentDecoded}")"

        local newFileContentEncoded="$(encode "${newFileContent}")"
        local fileSha="$(obtainFileSha "${fileData}")"

        local commitMessageWithVersion="$(replace "\*version\*" \
            "${targetVersionNoQuotes}" \
            "${commitMessage}")"

        commitAndPushFile "${commitMessageWithVersion}" \
            "${fullFilePath}" \
            "${newFileContentEncoded}" \
            "${fileSha}" \
            "${newBranchName}"

        log "Committed and pushed the file ${fileWithVersion}, commit message: \
            ${commitMessageWithVersion}."

        if [ "${branchExists}" = 'false' ]; then
            local pullRequestTitleWithVersion="$(replace "\*version\*" \
                "${targetVersionNoQuotes}" \
                "${pullRequestTitle}")"

            local pullRequestNumber="$(createPullRequest \
                "${pullRequestTitleWithVersion}" \
                "${newBranchName}" \
                "${branchToMergeInto}" \
                "${pullRequestBody}" \
                "${repositoryUrl}")"

            log "Created pull request with the title: ${pullRequestTitleWithVersion}, \
                head: ${newBranchName}, base: ${branchToMergeInto}, body: ${pullRequestBody} \
                in ${repositoryName}. Pull request number: ${pullRequestNumber}."

            assignPullRequest "${pullRequestNumber}" \
                "${repositoryUrl}" \
                "${pullRequestAssignee}"

            log "Assigned the pull request with the number ${pullRequestNumber} \
                to ${pullRequestAssignee}."
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
#   sourceVersionVariable - name of the variable representing the source version of the library
#   targetRepository - target repository URL via GitHub API
#   targetFileWithVersion - relative path to the configuration file containing target version
#   targetVersionVariable - name of the variable representing the target version of the library
#   newBranchName - name of the branch for the updated file; use "*version*" placeholder to insert the new version into it
#   branchToMergeInto - name of the branch to merge the updated version of the file into
#   commitMessage - title of the commit with the updated file; use "*version*" placeholder to insert the new version into it
#   pullRequestTitle - title of the new pull request; use "*version*" placeholder to insert the new version into it
#   pullRequestBody - body of the pull request for the new branch
#   pullRequestAssignee - assignee of the created pull request
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

    GIT_AUTHORIZATION_HEADER="Authorization: token ${gitAuthorizationToken}"

    local sourceRepositoryName="$(retrieveRepositoryName "${sourceRepository}")"

    log "Obtaining a version of ${sourceRepositoryName} from the config file \
        ${sourceFileWithVersion} from the variable ${sourceVersionVariable}."

    local targetVersion="$(obtainVersion "${sourceRepository}" \
        "${sourceFileWithVersion}" \
        "${sourceVersionVariable}")"

    log "The version is ${targetVersion}."

    updateVersion "${targetVersion}" \
        "${targetRepository}" \
        "${targetFileWithVersion}" \
        "${targetVersionVariable}" \
        "${newBranchName}" \
        "${branchToMergeInto}" \
        "${commitMessage}" \
        "${pullRequestTitle}" \
        "${pullRequestBody}" \
        "${pullRequestAssignee}"
}

main "$@"

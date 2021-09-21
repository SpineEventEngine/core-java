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

# This script is a part of a GitHub Actions workflow.
#
# Its purpose is to prevent PRs from leaving `pom.xml` and `license-report.md` from being untouched.
# In case any of these files are not modified, it exits with an error code 1.
# Otherwise, exits with a success code 0.
#
# In its implementation, the script relies into the environment variables set by GitHub Actions.
# See https://docs.github.com/en/actions/reference/environment-variables.


# Detects if the file with the passed name has been modified in this changeset.
# Exists with the code 1, if such a file has NOT been modified.
# Does nothing, if such a modification was found.
function ensureChanged() {

	modificationCount=$(git diff --name-only $GITHUB_HEAD_REF...$GITHUB_BASE_REF | grep $1 | wc -l)
	if [ "$modificationCount" -eq "0" ];
	then
	   echo "ERROR: '$1' file has not been modified in this PR. Please re-check the changeset.";
	   exit 1;
	else
		echo "Detected the modifications in '$1'."
	fi
}

echo "Starting to check if all required files were modified within this PR..."
echo "Comparing \"$GITHUB_HEAD_REF\" branch to \"$GITHUB_BASE_REF\" contents."

ensureChanged "pom.xml"
ensureChanged "license-report.md"

echo "All good."
exit 0;

/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.internal.gradle.publish

import io.spine.internal.gradle.Credentials
import io.spine.internal.gradle.Repository
import org.gradle.api.Project

/**
 * Maven repositories of Spine Event Engine projects hosted at GitHub Packages.
 */
internal object GitHubPackages {

    /**
     * Obtains an instance of the GitHub Packages repository with the given name.
     */
    fun repository(repoName: String): Repository {
        val githubActor: String = actor()
        return Repository(
            name = "GitHub Packages",
            releases = "https://maven.pkg.github.com/SpineEventEngine/$repoName",
            snapshots = "https://maven.pkg.github.com/SpineEventEngine/$repoName",
            credentialValues = { project -> project.credentialsWithToken(githubActor) }
        )
    }

    private fun actor(): String {
        var githubActor: String? = System.getenv("GITHUB_ACTOR")
        githubActor = if (githubActor.isNullOrEmpty()) {
            "developers@spine.io"
        } else {
            githubActor
        }
        return githubActor
    }
}

/**
 * This is a trick. Gradle only supports password or AWS credentials.
 * Thus, we pass the GitHub token as a "password".
 *
 * See https://docs.github.com/en/actions/guides/publishing-java-packages-with-gradle#publishing-packages-to-github-packages
 */
private fun Project.credentialsWithToken(githubActor: String) = Credentials(
    username = githubActor,
    password = readGitHubToken()
)

private fun Project.readGitHubToken(): String {
    val githubToken: String? = System.getenv("GITHUB_TOKEN")
    return if (githubToken.isNullOrEmpty()) {
        // Use the personal access token for the `developers@spine.io` account.
        // Only has the permission to read public GitHub packages.
        val targetDir = "${buildDir}/token"
        file(targetDir).mkdirs()
        val fileToUnzip = "${rootDir}/buildSrc/aus.weis"

        logger.info("GitHub Packages: reading token " +
                "by unzipping `$fileToUnzip` into `$targetDir`.")
        exec {
            // Unzip with password "123", allow overriding, quietly,
            // into the target dir, the given archive.
            commandLine("unzip", "-P", "123", "-oq", "-d", targetDir, fileToUnzip)
        }
        val file = file("$targetDir/token.txt")
        file.readText()
    } else {
        githubToken
    }
}

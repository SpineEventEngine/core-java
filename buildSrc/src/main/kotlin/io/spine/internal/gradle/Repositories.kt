/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.internal.gradle

import java.io.File
import java.net.URI
import java.util.*
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * A Maven repository.
 */
data class Repository(
    val releases: String,
    val snapshots: String,
    private val credentialsFile: String? = null,
    private val credentials: Credentials? = null,
    val name: String = "Maven repository `$releases`"
) {

    /**
     * Obtains the publishing password credentials to this repository.
     *
     * If the credentials are represented by a `.properties` file, reads the file and parses
     * the credentials. The file must have properties `user.name` and `user.password`, which store
     * the username and the password for the Maven repository auth.
     */
    fun credentials(project: Project): Credentials? {
        if (credentials != null) {
            return credentials
        }
        credentialsFile!!
        val log = project.logger
        log.info("Using credentials from `$credentialsFile`.")
        val file = project.rootProject.file(credentialsFile)
        if (!file.exists()) {
            return null
        }
        val creds = file.readCredentials()
        log.info("Publishing build as `${creds.username}`.")
        return creds
    }

    private fun File.readCredentials(): Credentials {
        val properties = Properties()
        properties.load(inputStream())
        val username = properties.getProperty("user.name")
        val password = properties.getProperty("user.password")
        return Credentials(username, password)
    }

    override fun toString(): String {
        return name
    }
}

/**
 * Password credentials for a Maven repository.
 */
data class Credentials(
    val username: String?,
    val password: String?
)

/**
 * Repositories to which we may publish. Normally, only one repository will be used.
 *
 * See `publish.gradle` for details of the publishing process.
 */
object PublishingRepos {

    @Suppress("HttpUrlsUsage") // HTTPS is not supported by this repository.
    val mavenTeamDev = Repository(
        name = "maven.teamdev.com",
        releases = "http://maven.teamdev.com/repository/spine",
        snapshots = "http://maven.teamdev.com/repository/spine-snapshots",
        credentialsFile = "credentials.properties"
    )
    val cloudRepo = Repository(
        name = "CloudRepo",
        releases = "https://spine.mycloudrepo.io/public/repositories/releases",
        snapshots = "https://spine.mycloudrepo.io/public/repositories/snapshots",
        credentialsFile = "cloudrepo.properties"
    )

    fun gitHub(repoName: String): Repository {
        return Repository(
            name = "GitHub Packages",
            releases = "https://maven.pkg.github.com/SpineEventEngine/$repoName",
            snapshots = "https://maven.pkg.github.com/SpineEventEngine/$repoName",
            credentials = Credentials(
                username = System.getenv("GITHUB_ACTOR"),
                // This is a trick. Gradle only supports password or AWS credentials. Thus,
                // we pass the GitHub token as a "password".
                // https://docs.github.com/en/actions/guides/publishing-java-packages-with-gradle#publishing-packages-to-github-packages
                password = System.getenv("GITHUB_TOKEN")
            )
        )
    }
}

/**
 * Defines names of additional repositories commonly used in the framework projects.
 *
 * @see [applyStandard]
 */
@Suppress("unused")
object Repos {
    val oldSpine: String = PublishingRepos.mavenTeamDev.releases
    val oldSpineSnapshots: String = PublishingRepos.mavenTeamDev.snapshots

    val spine: String = PublishingRepos.cloudRepo.releases
    val spineSnapshots: String = PublishingRepos.cloudRepo.snapshots

    const val sonatypeReleases: String = "https://oss.sonatype.org/content/repositories/snapshots"
    const val sonatypeSnapshots: String = "https://oss.sonatype.org/content/repositories/snapshots"
}

/**
 * The function to be used in `buildscript` clauses when fully-qualified call must be made.
 */
@Suppress("unused")
fun doApplyStandard(repositories: RepositoryHandler) {
    repositories.applyStandard()
}

/**
 * Applies repositories commonly used by Spine Event Engine projects.
 */
@Suppress("unused")
fun RepositoryHandler.applyStandard() {

    apply {
        gradlePluginPortal()
        mavenLocal()

        val libraryGroup = "io.spine"
        val toolsGroup = "io.spine.tools"
        val gcloudGroup = "io.spine.gcloud"

        maven {
            url = URI(Repos.spine)
            content {
                includeGroup(libraryGroup)
                includeGroup(toolsGroup)
                includeGroup(gcloudGroup)
            }
        }
        maven {
            url = URI(Repos.spineSnapshots)
            content {
                includeGroup(libraryGroup)
                includeGroup(toolsGroup)
                includeGroup(gcloudGroup)
            }
        }
        mavenCentral()
        maven {
            url = URI(Repos.sonatypeReleases)
        }
        maven {
            url = URI(Repos.sonatypeSnapshots)
        }
    }
}

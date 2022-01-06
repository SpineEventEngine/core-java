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

package io.spine.internal.gradle

import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.PublishingRepos.gitHub
import java.io.File
import java.net.URI
import java.util.*
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

/**
 * A Maven repository.
 */
data class Repository(
    val releases: String,
    val snapshots: String,
    private val credentialsFile: String? = null,
    private val credentialValues: ((Project) -> Credentials?)? = null,
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
        if (credentialValues != null) {
            return credentialValues.invoke(project)
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
 * Defines names of additional repositories commonly used in the framework projects.
 *
 * @see [applyStandard]
 */
@Suppress("unused")
object Repos {
    @Deprecated(
        message = "Please use another repository.",
        replaceWith = ReplaceWith("artifactRegistry"),
        level = DeprecationLevel.ERROR
    )
    val oldSpine = PublishingRepos.mavenTeamDev.releases

    @Deprecated(
        message = "Please use another repository.",
        replaceWith = ReplaceWith("artifactRegistrySnapshots"),
        level = DeprecationLevel.ERROR
    )
    val oldSpineSnapshots = PublishingRepos.mavenTeamDev.snapshots

    val spine = PublishingRepos.cloudRepo.releases
    val spineSnapshots = PublishingRepos.cloudRepo.snapshots

    val artifactRegistry = PublishingRepos.cloudArtifactRegistry.releases
    val artifactRegistrySnapshots = PublishingRepos.cloudArtifactRegistry.snapshots

    @Deprecated(
        message = "Sonatype release repository redirects to the Maven Central",
        replaceWith = ReplaceWith("sonatypeSnapshots"),
        level = DeprecationLevel.ERROR
    )
    const val sonatypeReleases = "https://oss.sonatype.org/content/repositories/snapshots"
    const val sonatypeSnapshots = "https://oss.sonatype.org/content/repositories/snapshots"
}

/**
 * Registers the standard set of Maven repositories.
 *
 * To be used in `buildscript` clauses when a fully-qualified call must be made.
 */
@Suppress("unused")
fun doApplyStandard(repositories: RepositoryHandler) = repositories.applyStandard()

/**
 * Registers the selected GitHub Packages repos as Maven repositories.
 *
 * To be used in `buildscript` clauses when a fully-qualified call must be made.
 *
 * @param repositories
 *          the handler to accept registration of the GitHub Packages repository
 * @param shortRepositoryName
 *          the short name of the GitHub repository (e.g. "core-java")
 * @param project
 *          the project which is going to consume or publish artifacts from
 *          the registered repository
 * @see applyGitHubPackages
 */
@Suppress("unused")
fun doApplyGitHubPackages(
    repositories: RepositoryHandler,
    shortRepositoryName: String,
    project: Project
) = repositories.applyGitHubPackages(shortRepositoryName, project)

/**
 * Applies the repositories hosted at GitHub Packages, to which Spine artifacts were published.
 *
 * This method should be used by those wishing to have Spine artifacts published
 * to GitHub Packages as dependencies.
 *
 * @param shortRepositoryName
 *          the short name of the GitHub repository (e.g. "core-java")
 * @param project
 *          the project which is going to consume or publish artifacts from
 *          the registered repository
 */
fun RepositoryHandler.applyGitHubPackages(shortRepositoryName: String, project: Project) {
    val repository = gitHub(shortRepositoryName)
    val credentials = repository.credentials(project)

    credentials?.let {
        spineMavenRepo(it, repository.releases)
        spineMavenRepo(it, repository.snapshots)
    }
}

/**
 * Applies repositories commonly used by Spine Event Engine projects.
 *
 * Does not include the repositories hosted at GitHub Packages.
 *
 * @see applyGitHubPackages
 */
@Suppress("unused")
fun RepositoryHandler.applyStandard() {

    gradlePluginPortal()
    mavenLocal()

    val spineRepos = listOf(
        Repos.spine,
        Repos.spineSnapshots,
        Repos.artifactRegistry,
        Repos.artifactRegistrySnapshots
    )

    spineRepos
        .map { URI(it) }
        .forEach {
            maven {
                url = it
                includeSpineOnly()
            }
        }

    mavenCentral()
    maven {
        url = URI(Repos.sonatypeSnapshots)
    }
}

/**
 * Registers the Maven repository with the passed [repoCredentials] for authorization.
 *
 * Only includes the Spine-related artifact groups.
 */
private fun RepositoryHandler.spineMavenRepo(
    repoCredentials: Credentials,
    repoUrl: String
) {
    maven {
        url = URI(repoUrl)
        includeSpineOnly()
        credentials {
            username = repoCredentials.username
            password = repoCredentials.password
        }
    }
}

/**
 * Narrows down the search for this repository to Spine-related artifact groups.
 */
private fun MavenArtifactRepository.includeSpineOnly() {
    content {
        includeGroupByRegex("io\\.spine.*")
    }
}

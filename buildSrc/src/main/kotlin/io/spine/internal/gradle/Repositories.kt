/*
 * Copyright 2023, TeamDev. All rights reserved.
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

@file:Suppress("TooManyFunctions") // Deprecated functions will be kept for a while.

package io.spine.internal.gradle

import io.spine.internal.gradle.publish.CloudRepo
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.PublishingRepos.gitHub
import java.io.File
import java.net.URI
import java.util.*
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.kotlin.dsl.ScriptHandlerScope
import org.gradle.kotlin.dsl.maven

/**
 * Applies [standard][doApplyStandard] repositories to this [ScriptHandlerScope]
 * optionally adding [gitHub] repositories for Spine-only components, if
 * names of such repositories are given.
 *
 * @param buildscript
 *         a [ScriptHandlerScope] to work with. Pass `this` under `buildscript { }`.
 * @param rootProject
 *         a root project where the `buildscript` is declared.
 * @param gitHubRepo
 *         a list of short repository names, or empty list if only
 *         [standard repositories][doApplyStandard] are required.
 */
@Suppress("unused")
@Deprecated(
    message = "Please use `standardSpineSdkRepositories()`.",
    replaceWith = ReplaceWith("standardSpineSdkRepositories()")
)
fun applyWithStandard(
    buildscript: ScriptHandlerScope,
    rootProject: Project,
    vararg gitHubRepo: String
) {
    val repositories = buildscript.repositories
    gitHubRepo.iterator().forEachRemaining { repo ->
        repositories.applyGitHubPackages(repo, rootProject)
    }
    repositories.standardToSpineSdk()
}

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
 *          the project which is going to consume artifacts from the repository
 * @see applyGitHubPackages
 */
@Suppress("unused")
@Deprecated(
    message = "Please use `standardSpineSdkRepositories()`.",
    replaceWith = ReplaceWith("standardSpineSdkRepositories()")
)
fun doApplyGitHubPackages(
    repositories: RepositoryHandler,
    shortRepositoryName: String,
    project: Project
) = repositories.applyGitHubPackages(shortRepositoryName, project)

/**
 * Registers the standard set of Maven repositories.
 *
 * To be used in `buildscript` clauses when a fully-qualified call must be made.
 */
@Suppress("unused")
@Deprecated(
    message = "Please use `standardSpineSdkRepositories()`.",
    replaceWith = ReplaceWith("standardSpineSdkRepositories()")
)
fun doApplyStandard(repositories: RepositoryHandler) = repositories.standardToSpineSdk()

/**
 * Applies the repository hosted at GitHub Packages, to which Spine artifacts were published.
 *
 * This method should be used by those wishing to have Spine artifacts published
 * to GitHub Packages as dependencies.
 *
 * @param shortRepositoryName
 *          short names of the GitHub repository (e.g. "base", "core-java", "model-tools")
 * @param project
 *          the project which is going to consume artifacts from repositories
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
fun RepositoryHandler.applyGitHubPackages(project: Project, vararg shortRepositoryName: String) {
    for (name in shortRepositoryName) {
        applyGitHubPackages(name, project)
    }
}

/**
 * Applies [standard][applyStandard] repositories to this [RepositoryHandler]
 * optionally adding [applyGitHubPackages] repositories for Spine-only components, if
 * names of such repositories are given.
 *
 * @param project
 *         a project to which we add dependencies
 * @param gitHubRepo
 *         a list of short repository names, or empty list if only
 *         [standard repositories][applyStandard] are required.
 */
@Suppress("unused")
@Deprecated(
    message = "Please use `standardToSpineSdk()`.",
    replaceWith = ReplaceWith("standardToSpineSdk()")
)
fun RepositoryHandler.applyStandardWithGitHub(project: Project, vararg gitHubRepo: String) {
    gitHubRepo.iterator().forEachRemaining { repo ->
        applyGitHubPackages(repo, project)
    }
    standardToSpineSdk()
}

/**
 * A scrambled version of PAT generated with the only "read:packages" scope.
 *
 * The scrambling around PAT is necessary because GitHub analyzes commits for the presence
 * of tokens and invalidates them.
 *
 * @see <a href="https://github.com/orgs/community/discussions/25629">
 *     How to make GitHub packages to the public</a>
 */
object Pat {
    private const val shade = "_phg->8YlN->MFRA->gxIk->HVkm->eO6g->FqHJ->z8MS->H4zC->ZEPq"
    private const val separator = "->"
    private val chunks: Int = shade.split(separator).size - 1

    fun credentials(): Credentials {
        val pass = shade.replace(separator, "").splitAndReverse(chunks, "")
        return Credentials("public", pass)
    }

    /**
     * Splits this string to the chunks, reverses each chunk, and joins them
     * back to a string using the [separator].
     */
    private fun String.splitAndReverse(numChunks: Int, separator: String): String {
        check(length / numChunks >= 2) {
            "The number of chunks is too big. Must be <= ${length / 2}."
        }
        val chunks = chunked(length / numChunks)
        val reversedChunks = chunks.map { chunk -> chunk.reversed() }
        return reversedChunks.joinToString(separator)
    }
}

/**
 * Adds a read-only view to all artifacts of the SpineEventEngine
 * GitHub organization.
 */
fun RepositoryHandler.spineArtifacts(): MavenArtifactRepository = maven {
    url = URI("https://maven.pkg.github.com/SpineEventEngine/*")
    includeSpineOnly()
    val pat = Pat.credentials()
    credentials {
        username = pat.username
        password = pat.password
    }
}

val RepositoryHandler.intellijReleases: MavenArtifactRepository
    get() = maven("https://www.jetbrains.com/intellij-repository/releases")

val RepositoryHandler.jetBrainsCacheRedirector: MavenArtifactRepository
    get() = maven("https://cache-redirector.jetbrains.com/intellij-dependencies")

/**
 * Applies repositories commonly used by Spine Event Engine projects.
 */
fun RepositoryHandler.standardToSpineSdk() {
    spineArtifacts()

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

    intellijReleases
    jetBrainsCacheRedirector

    maven {
        url = URI(Repos.sonatypeSnapshots)
    }

    mavenCentral()
    gradlePluginPortal()
    mavenLocal().includeSpineOnly()
}

@Deprecated(
    message = "Please use `standardToSpineSdk() instead.",
    replaceWith = ReplaceWith("standardToSpineSdk()")
)
fun RepositoryHandler.applyStandard() = this.standardToSpineSdk()

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
    fun credentials(project: Project): Credentials? = when {
        credentialValues != null -> credentialValues.invoke(project)
        credentialsFile != null -> credsFromFile(credentialsFile, project)
        else -> throw IllegalArgumentException(
            "Credentials file or a supplier function should be passed."
        )
    }

    private fun credsFromFile(fileName: String, project: Project): Credentials? {
        val file = project.rootProject.file(fileName)
        if (file.exists().not()) {
            return null
        }

        val log = project.logger
        log.info("Using credentials from `$fileName`.")
        val creds = file.parseCredentials()
        log.info("Publishing build as `${creds.username}`.")
        return creds
    }

    private fun File.parseCredentials(): Credentials {
        val properties = Properties().apply { load(inputStream()) }
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
 * Defines names of additional repositories commonly used in the Spine SDK projects.
 *
 * @see [applyStandard]
 */
private object Repos {
    val spine = CloudRepo.published.releases
    val spineSnapshots = CloudRepo.published.snapshots
    val artifactRegistry = PublishingRepos.cloudArtifactRegistry.releases
    val artifactRegistrySnapshots = PublishingRepos.cloudArtifactRegistry.snapshots

    @Suppress("unused")
    @Deprecated(
        message = "Sonatype release repository redirects to the Maven Central",
        replaceWith = ReplaceWith("sonatypeSnapshots"),
        level = DeprecationLevel.ERROR
    )
    const val sonatypeReleases = "https://oss.sonatype.org/content/repositories/snapshots"
    const val sonatypeSnapshots = "https://oss.sonatype.org/content/repositories/snapshots"
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

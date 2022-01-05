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

import io.spine.internal.gradle.Repository
import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.PublishArtifactSet
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

/**
 * Creates a `MavenPublication` in the given project, taking custom parameters from
 * given `PublishExtension`.
 */
internal fun PublishingExtension.createMavenPublication(
    project: Project,
    extension: PublishExtension
) {
    val artifactIdForPublishing = extension.artifactId(project)
    publications {
        create("mavenJava", MavenPublication::class.java) {
            groupId = project.group.toString()
            artifactId = artifactIdForPublishing
            version = project.version.toString()

            from(project.components.getAt("java"))

            val archivesConfig = project.configurations.getAt(ConfigurationName.archives)
            val allArtifacts = archivesConfig.allArtifacts
            val deduplicated = allArtifacts.deduplicate()
            setArtifacts(deduplicated)
        }
    }
}

/**
 * Obtains an [Iterable] containing artifacts that have the same `extension` and `classifier`.
 *
 * Such a situation may occur when applying both `com.gradle.plugin-publish` plugin AND
 * `spinePublishing` in the same project. `com.gradle.plugin-publish` adds `sources` and `javadoc`
 * artifacts, and we do it too in [Project.setUpDefaultArtifacts].
 *
 * At the time when we add artifacts in [Project.setUpDefaultArtifacts], those added by
 * `com.gradle.plugin-publish` are not yet visible to our code. Hence, we have to perform
 * the deduplication before we set the artifacts in [PublishingExtension.createMavenPublication].
 */
private fun PublishArtifactSet.deduplicate(): Iterable<PublishArtifact> {
    val deduplicated = mutableSetOf<PublishArtifact>()
    for (artifact in this) {
        deduplicated.stream()
            .filter {
                artifact.extension == it.extension && artifact.classifier == it.classifier
            }.findFirst()
            .ifPresent(deduplicated::remove)
        deduplicated.add(artifact)
    }
    return deduplicated
}

internal fun PublishingExtension.setUpRepositories(
    project: Project,
    extension: PublishExtension
) {
    val snapshots = project.version
        .toString()
        .matches(Regex(".+[-.]SNAPSHOT([+.]\\d+)?"))
    repositories {
        extension.targetRepositories.get().forEach { repo ->
            maven {
                initialize(repo, project, snapshots)
            }
        }
    }
}

private fun MavenArtifactRepository.initialize(
    repo: Repository,
    project: Project,
    snapshots: Boolean
) {
    val publicRepo = if (snapshots) {
        repo.snapshots
    } else {
        repo.releases
    }
    // Special treatment for CloudRepo URL.
    // Reading is performed via public repositories, and publishing via
    // private ones that differ in the `/public` infix.
    url = project.uri(publicRepo.replace("/public", ""))
    val creds = repo.credentials(project.rootProject)
    credentials {
        username = creds?.username
        password = creds?.password
    }
}

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

package io.spine.internal.gradle.github.pages

import java.io.File
import java.nio.file.Path
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import io.spine.internal.gradle.git.Repository

/**
 * Performs the update of GitHub pages.
 */
fun Task.updateGhPages(project: Project) {
    val plugin = project.plugins.getPlugin(UpdateGitHubPages::class.java)

    with(plugin) {
        SshKey(rootFolder).register()
    }

    val repository = Repository.forPublishingDocumentation()

    val updateJavadoc = with(plugin) {
        UpdateJavadoc(project, javadocOutputFolder, repository, logger)
    }

    val updateDokka = with(plugin) {
        UpdateDokka(project, dokkaOutputFolder, repository, logger)
    }

    repository.use {
        updateJavadoc.run()
        updateDokka.run()
        repository.push()
    }
}

private abstract class UpdateDocumentation(
    private val project: Project,
    private val docsSourceFolder: Path,
    private val repository: Repository,
    private val logger: Logger
) {

    /**
     * The folder under the repository's root(`/`) for storing documentation.
     *
     * The value should not contain any leading or trailing file separators.
     *
     * The absolute path to the project's documentation is made by appending its
     * name to the end, making `/[docsDestinationFolder]/[project.name]`.
     */
    protected abstract val docsDestinationFolder: String

    /**
     * The name of the tool used to generate the documentation to update.
     *
     * This name will appear in logs as part of a message.
     */
    protected abstract val toolName: String

    private val mostRecentFolder = File("${repository.location}/${docsDestinationFolder}/${project.name}")

    fun run() {
        logger.debug("Update of the ${toolName} documentation for module `${project.name}` started.")

        val documentation = replaceMostRecentDocs()
        copyIntoVersionDir(documentation)

        val updateMessage = "Update ${toolName} documentation for module `${project.name}` as for " +
                "version ${project.version}"
        repository.commitAllChanges(updateMessage)

        logger.debug("Update of the ${toolName} documentation for `${project.name}` successfully finished.")
    }

    private fun replaceMostRecentDocs(): ConfigurableFileCollection {
        val generatedDocs = project.files(docsSourceFolder)

        logger.debug("Replacing the most recent ${toolName} documentation in ${mostRecentFolder}.")
        copyDocs(generatedDocs, mostRecentFolder)

        return generatedDocs
    }

    private fun copyDocs(source: FileCollection, destination: File) {
        destination.mkdir()
        project.copy {
            from(source)
            into(destination)
        }
    }

    private fun copyIntoVersionDir(generatedDocs: ConfigurableFileCollection) {
        val versionedDocDir = File("$mostRecentFolder/v/${project.version}")

        logger.debug("Storing the new version of ${toolName} documentation in `${versionedDocDir}.")
        copyDocs(generatedDocs, versionedDocDir)
    }
}

private class UpdateJavadoc(
    project: Project,
    docsSourceFolder: Path,
    repository: Repository,
    logger: Logger
) : UpdateDocumentation(project, docsSourceFolder, repository, logger) {

    override val docsDestinationFolder: String
        get() = "reference"
    override val toolName: String
        get() = "Javadoc"
}

private class UpdateDokka(
    project: Project,
    docsSourceFolder: Path,
    repository: Repository,
    logger: Logger
) : UpdateDocumentation(project, docsSourceFolder, repository, logger) {

    override val docsDestinationFolder: String
        get() = "dokka-reference"
    override val toolName: String
        get() = "Dokka"
}

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

package io.spine.internal.gradle

import java.util.*
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * A task that generates a dependency versions `.properties` file.
 */
abstract class WriteVersions : DefaultTask() {

    /**
     * Versions to add to the file.
     *
     * The map key is a string in the format of `<group ID>_<artifact name>`, and the value
     * is the version corresponding to those group ID and artifact name.
     *
     * @see WriteVersions.version
     */
    @get:Input
    abstract val versions: MapProperty<String, String>

    /**
     * The directory that hosts the generated file.
     */
    @get:OutputDirectory
    abstract val versionsFileLocation: DirectoryProperty

    /**
     * Adds a dependency version to write into the file.
     *
     * The given dependency notation is a Gradle artifact string of format:
     * `"<group ID>:<artifact name>:<version>"`.
     *
     * @see WriteVersions.versions
     * @see WriteVersions.includeOwnVersion
     */
    fun version(dependencyNotation: String) {
        val parts = dependencyNotation.split(":")
        check(parts.size == 3) { "Invalid dependency notation: `$dependencyNotation`." }
        versions.put("${parts[0]}_${parts[1]}", parts[2])
    }

    /**
     * Enables the versions file to include the version of the project that owns this task.
     *
     * @see WriteVersions.version
     * @see WriteVersions.versions
     */
    fun includeOwnVersion() {
        val groupId = project.group.toString()
        val artifactId = project.artifactId
        val version = project.version.toString()
        versions.put("${groupId}_${artifactId}", version)
    }

    /**
     * Creates a `.properties` file with versions, named after the value
     * of [Project.artifactId] property.
     *
     * The name of the file would be: `versions-<artifactId>.properties`.
     *
     * By default, value of [Project.artifactId] property is a project's name with "spine-" prefix.
     * For example, if a project's name is "tools", then the name of the file would be:
     * `versions-spine-tools.properties`.
     */
    @TaskAction
    fun writeFile() {
        versions.finalizeValue()
        versionsFileLocation.finalizeValue()

        val values = versions.get()
        val properties = Properties()
        properties.putAll(values)
        val outputDir = versionsFileLocation.get().asFile
        outputDir.mkdirs()
        val fileName = resourceFileName()
        val file = outputDir.resolve(fileName)
        file.createNewFile()
        file.writer().use {
            properties.store(it, "Dependency versions supplied by the `$path` task.")
        }
    }

    private fun resourceFileName(): String {
        val artifactId = project.artifactId
        return "versions-${artifactId}.properties"
    }
}

/**
 * A plugin that enables storing dependency versions into a resource file.
 *
 * Dependency version may be used by Gradle plugins at runtime.
 *
 * The plugin adds one task — `writeVersions`, which generates a `.properties` file with some
 * dependency versions.
 *
 * The generated file will be available in classpath of the target project under the name:
 * `versions-<project name>.properties`, where `<project name>` is the name of the target
 * Gradle project.
 */
@Suppress("unused")
class VersionWriter : Plugin<Project> {

    override fun apply(target: Project): Unit = with (target.tasks) {
        val task = register("writeVersions", WriteVersions::class.java) {
            versionsFileLocation.convention(project.layout.buildDirectory.dir(name))
            includeOwnVersion()
            project.sourceSets
                .getByName("main")
                .resources
                .srcDir(versionsFileLocation)
        }
        getByName("processResources").dependsOn(task)
    }
}

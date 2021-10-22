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

package io.spine.internal.gradle.publish.proto

import io.spine.internal.gradle.sourceSets
import java.io.File
import java.util.*
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet

/**
 * Extensions assisting in locating `.proto` files and directories in a Gradle `Project`.
 */

/**
 * Collects all the directories from this project and its dependencies (including zip tree
 * directories) which contain `.proto` definitions.
 *
 * The directories may in practice include files of other extension. The caller should take care
 * of handling those files respectively.
 *
 * It's guaranteed that there are no other Proto definitions in the current project classpath
 * except those included into the returned `Collection`.
 */
internal fun Project.protoFiles(): Collection<File> {
    val files = this.configurations.findByName("runtimeClasspath")!!.files
    val jarFiles = files.map { JarFileName(it.name) }
    val result = mutableListOf<File>()
    files.filter { it.name.endsWith(".jar") }.forEach { file ->
        val tree = zipTree(file)
        try {
            tree.filter { it.isProto() }.forEach {
                val protoRoot = it.findProtoRoot(jarFiles)
                result.add(protoRoot)
            }
        } catch (e: GradleException) {
            /*
             * As the `:assembleProto` task configuration is resolved first upon the project
             * configuration (and we don't have the dependencies there yet) and then upon
             * the execution, the task should complete successfully.
             *
             * To make sure the configuration phase passes, we suppress the GradleException
             * thrown by `zipTree()` indicating that the given file, which is a dependency JAR
             * file does not exist.
             *
             * Though, if this error is thrown on the execution phase, this IS an error. Thus,
             * we log an error message.
             *
             * As a side effect, the message is shown upon `./gradlew clean build` or upon
             * a newly created version of framework build etc.
             */
            this@protoFiles.logger.debug(
                "${e.message}${System.lineSeparator()}" +
                        "The proto artifact may be corrupted."
            )
        }
    }
    val mainSourceSet = this.sourceSets.findByName("main")!!
    val protoSourceDirs = mainSourceSet.extensions.getByName("proto") as SourceDirectorySet
    result.addAll(protoSourceDirs.srcDirs)
    return result
}

/**
 * Tells whether this file represents either a `.proto` file, or a directory containing proto files.
 *
 * If this file is a directory, scans its children recursively.
 *
 * @return `true` if the this [is a Protobuf file][isProto], or
 *         a directory containing at least one Protobuf file,
 *         `false` otherwise
 */
internal fun File.isProtoFileOrDir(): Boolean {
    val filesToCheck = LinkedList<File>()
    filesToCheck.push(this)
    if (this.isDirectory && this.list()?.isEmpty() == true) {
        return false
    }
    while (!filesToCheck.isEmpty()) {
        val file = filesToCheck.pop()
        if (file.isProto()) {
            return true
        }
        if (file.isDirectory) {
            file.listFiles()?.forEach { filesToCheck.push(it) }
        }
    }
    return false
}

/**
 * Tells whether this file is a `.proto` file.
 */
private fun File.isProto(): Boolean {
    return this.isFile && this.name.endsWith(".proto")
}

private fun File.findProtoRoot(archiveContents: Collection<JarFileName>): File {
    var pkg = this
    while (!archiveContents.contains(pkg.parentFile.jarName())) {
        pkg = pkg.parentFile
    }
    return pkg.parentFile
}

/**
 * Retrieves the name of the this folder trimmed by `.jar` suffix.
 *
 * //TODO:2021-10-21:alex.tymchenko: this description does not seem right.
 *
 * More formally, returns the name of this `File` if the name does not contain
 * `.jar` substring or the substring of the name containing the characters from the start
 * to the `.jar` sequence (inclusively).
 *
 * This transformation corresponds to finding the name of a JAR file which was extracted to
 * the given directory with Gradle `zipTree()` API.
 */
private fun File.jarName(): JarFileName? {
    val unpackedJarInfix = ".jar"
    val name = this.name
    val index = name.lastIndexOf(unpackedJarInfix)
    return if (index < 0) {
        null
    } else {
        JarFileName(name.substring(0, index + unpackedJarInfix.length))
    }
}

/**
 * The filename of a JAR dependency of the project.
 */
private data class JarFileName(val name: String)

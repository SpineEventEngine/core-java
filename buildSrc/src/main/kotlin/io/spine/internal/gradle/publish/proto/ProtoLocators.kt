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
                val protoRoot = it.rootFolderIn(jarFiles)
                result.add(protoRoot)
            }
        } catch (e: GradleException) {
            /*
             * This code may be executed at the `configuration` phase of a Gradle project.
             * If that's a very first configuration attempt (for instance, a newly created library
             * version of the framework is being built), this exception is very likely to be
             * thrown. It is so, as we don't have any dependencies fetched yet.
             *
             * However, we want the `configuration` phase to complete successfully. Therefore,
             * here we suppress the `GradleException` throw by `zipTree()`. We believe that
             * down the road, in the following stages of build execution the dependencies will
             * be fetched, so that `zipTree()` won't fail anymore.
             *
             * As a side effect, the message is shown upon `./gradlew clean build` or upon
             * a newly created version of framework build etc.
             *
             * However, if this exception is thrown on the execution phase (very much after
             * the initial configuration is completed), this IS an error.
             * Thus, we log an error message. Framework users should pay attention
             * to the circumstances under which this message is logged.
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
 * Tells whether this file represents either a `.proto` file, or a directory containing Proto files.
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

/**
 * Assuming this file is a part of examined JAR dependencies, returns the root folder
 * to which this file belongs.
 *
 * In particular, this may be used to find the Proto root folder for a given `.proto` file,
 * which originally was packed into one of Gradle project's dependencies.
 */
private fun File.rootFolderIn(archiveContents: Collection<JarFileName>): File {
    var pkg = this
    while (!archiveContents.contains(pkg.parentFile.jarName())) {
        pkg = pkg.parentFile
    }
    return pkg.parentFile
}

/**
 * Retrieves the name of the `.jar` to which this file belongs, when examining the file through
 * Gradle's `zipTree()` API.
 *
 * The `.jar` suffix is included into the resulting value.
 *
 * If there is no `.jar` substring in the name of this file, returns `null`.
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

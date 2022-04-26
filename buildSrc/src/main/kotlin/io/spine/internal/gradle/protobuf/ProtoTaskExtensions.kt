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

package io.spine.internal.gradle.protobuf

import com.google.protobuf.gradle.GenerateProtoTask
import java.io.File
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.get

/**
 * Configures protobuf code generation task.
 *
 * The task configuration consists of the following steps.
 *
 * 1. Generation of descriptor set file is turned op for each source set.
 *    These files are placed under the `build/descriptors` directory.
 *
 * 2. At the final steps of the code generation, the code belonging to
 *    the `com.google` package is removed.
 *
 * 3. Make `processResource` tasks depend on corresponding `generateProto` tasks.
 *    If the source set of the configured task isn't `main`, appropriate infix for
 *    the task names is used.
 *
 * The usage of this extension in a <em>module build file</em> would be:
 * ```
 *  val generatedDir by extra("$projectDir/generated")
 *  protobuf {
 *      generateProtoTasks {
 *         for (task in all()) {
 *            task.setup(generatedDir)
 *         }
 *     }
 * }
 * ```
 * Using the same code under `subprojects` in a root build file does not seem work because
 * test descriptor set files are not copied to resources. Performing this configuration from
 * subprojects solves the issue.
 */
@Suppress("unused")
fun GenerateProtoTask.setup(generatedDir: String) {

    /**
     * Generate descriptor set files.
     */
    val ssn = sourceSet.name
    generateDescriptorSet = true
    with(descriptorSetOptions) {
        path = "${project.buildDir}/descriptors/${ssn}/known_types_${ssn}.desc"
        includeImports = true
        includeSourceInfo = true
    }

    /**
     * Remove the code generated for Google Protobuf library types.
     *
     * Java code for the `com.google` package was generated because we wanted
     * to have descriptors for all the types, including those from Google Protobuf library.
     * We want all the descriptors so that they are included into the resources used by
     * the `io.spine.type.KnownTypes` class.
     *
     * Now, as we have the descriptors _and_ excessive Java code, we delete it to avoid
     * classes that duplicate those coming from Protobuf library JARs.
     */
    doLast {
        val comPackage = File("${generatedDir}/${ssn}/java/com")
        val googlePackage = comPackage.resolve("google")

        project.delete(googlePackage)

        // We don't need an empty `com` package.
        if (comPackage.exists() && comPackage.list()?.isEmpty() == true) {
            project.delete(comPackage)
        }
    }

    /**
     * Make the tasks `processResources` depend on `generateProto` tasks explicitly so that:
     *  1) descriptor set files get into resources, avoiding the racing conditions
     *     during the build.
     *  2) we don't have the warning "Execution optimizations have been disabled..." issued
     *     by Gradle during the build because Protobuf Gradle Plugin does not set
     *     dependencies between `generateProto` and `processResources` tasks.
     */
    val processResources = processResourceTaskName(ssn)
    project.tasks[processResources].dependsOn(this)
}

/**
 * Obtains the name of the task `processResource` task for the given source set name.
 */
fun processResourceTaskName(sourceSetName: String): String {
    val infix = if (sourceSetName == "main") "" else sourceSetName.capitalized()
    return "process${infix}Resources"
}

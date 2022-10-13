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

import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Configures the dependencies between third-party Gradle tasks
 * and those defined via ProtoData and Spine Model Compiler.
 *
 * It is required in order to avoid warnings in build logs, detecting the undeclared
 * usage of Spine-specific task output by other tasks,
 * e.g. the output of `launchProtoDataMain` is used by `compileKotlin`.
 */
@Suppress("unused")
fun Project.configureTaskDependencies() {

    /**
     * Creates a dependency between the Gradle task of *this* name
     * onto the task with `taskName`.
     *
     * If either of tasks does not exist in the enclosing `Project`,
     * this method does nothing.
     *
     * This extension is kept local to `configureTaskDependencies` extension
     * to prevent its direct usage from outside.
     */
    fun String.dependOn(taskName: String) {
        val whoDepends = this
        val dependOntoTask: Task? = tasks.findByName(taskName)
        dependOntoTask?.let {
            tasks.findByName(whoDepends)?.dependsOn(it)
        }
    }

    afterEvaluate {
        "compileKotlin".dependOn("launchProtoDataMain")
        "compileTestKotlin".dependOn("launchProtoDataTest")
        "sourcesJar".dependOn("generateProto")
        "sourcesJar".dependOn("launchProtoDataMain")
        "sourcesJar".dependOn("createVersionFile")
        "dokkaHtml".dependOn("generateProto")
        "dokkaHtml".dependOn("launchProtoDataMain")
        "dokkaJavadoc".dependOn("launchProtoDataMain")
        "publishPluginJar".dependOn("createVersionFile")
    }
}

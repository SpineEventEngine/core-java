import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.withGroovyBuilder

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

/**
 * The following lines are the workaround
 * for https://github.com/SpineEventEngine/ProtoData/issues/72.
 *
 * We have to move the code residing in `<project>/build/generated-proto/<sourceset>/spine`
 * to the usual location of `<project>/generated/<sourceset>/spine` to avoid
 * numerous issues.
 *
 * This code should be removed, once the issue is resolved.
 */
fun Project.moveGeneratedProtoFromBuildFolder() {
    listOf<String>("main", "test").forEach { scope ->
        tasks.named("launchProtoData${scope.capitalize()}") {
            doLast {
                this.ant.withGroovyBuilder {
                    "move"(
                        "file" to "${buildDir}/generated-proto/$scope/spine",
                        "todir" to "${projectDir}/generated/${scope}/spine",

                        // In case the source folder does not exist, prevent failures.
                        "failonerror" to "false"
                    )
                }
            }
        }
    }
}

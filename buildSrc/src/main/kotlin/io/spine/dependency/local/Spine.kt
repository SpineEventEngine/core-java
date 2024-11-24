/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.dependency.local

/**
 * Dependencies on smaller Spine modules.
 */
@Suppress("unused", "ConstPropertyName")
object Spine {

    const val group = "io.spine"
    const val toolsGroup = "io.spine.tools"

    const val base = "$group:spine-base:${ArtifactVersion.base}"
    const val baseForBuildScript = "$group:spine-base:${ArtifactVersion.baseForBuildScript}"

    const val reflect = "$group:spine-reflect:${ArtifactVersion.reflect}"
    const val baseTypes = "$group:spine-base-types:${ArtifactVersion.baseTypes}"
    const val time = "$group:spine-time:${ArtifactVersion.time}"
    const val change = "$group:spine-change:${ArtifactVersion.change}"
    const val text = "$group:spine-text:${ArtifactVersion.text}"

    const val testlib = "$toolsGroup:spine-testlib:${ArtifactVersion.testlib}"
    const val testUtilTime = "$toolsGroup:spine-testutil-time:${ArtifactVersion.time}"

    @Deprecated(message = "Please use `ToolBase.psiJava` instead`.")
    const val psiJava = "$toolsGroup:spine-psi-java:${ArtifactVersion.toolBase}"
    @Deprecated(message = "Please use `ToolBase.psiJava` instead`.")
    const val psiJavaBundle = "$toolsGroup:spine-psi-java-bundle:${ArtifactVersion.toolBase}"
    @Deprecated(message = "Please use `ToolBase.lib` instead`.")
    const val toolBase = "$toolsGroup:spine-tool-base:${ArtifactVersion.toolBase}"
    @Deprecated(message = "Please use `ToolBase.pluginBase` instead`.")
    const val pluginBase = "$toolsGroup:spine-plugin-base:${ArtifactVersion.toolBase}"
    @Deprecated(message = "Please use `ToolBase.pluginTestlib` instead`.")
    const val pluginTestlib = "$toolsGroup:spine-plugin-testlib:${ArtifactVersion.toolBase}"

    const val modelCompiler = "$toolsGroup:spine-model-compiler:${ArtifactVersion.mc}"

    @Deprecated(
        message = "Please use top level `McJava` object instead.",
        ReplaceWith("McJava", "io.spine.dependency.local.McJava")
    )
    val McJava = io.spine.dependency.local.McJava

    const val javadocFilter = "$toolsGroup:spine-javadoc-filter:${ArtifactVersion.javadocTools}"

    @Deprecated(
        message = "Please use top level `CoreJava.client` object instead.",
        ReplaceWith("CoreJava.client", "io.spine.dependency.local.CoreJava")
    )
    const val client = CoreJava.client

    @Deprecated(
        message = "Please use top level `CoreJava.server` object instead.",
        ReplaceWith("CoreJava.server", "io.spine.dependency.local.CoreJava")
    )
    const val server = CoreJava.server
}

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

    @Deprecated(message = "Please use `Base.lib`.", ReplaceWith("Base.lib"))
    const val base = Base.lib

    @Deprecated(
        message = "Please use `Base.libForBuildScript`.",
        ReplaceWith("Base.libForBuildScript")
    )
    const val baseForBuildScript = Base.libForBuildScript

    @Deprecated(message = "Please use `Reflect.lib`.", ReplaceWith("Reflect.lib"))
    const val reflect = Reflect.lib

    @Deprecated(message = "Please use `BaseTypes.lib`.", ReplaceWith("BaseTypes.lib"))
    const val baseTypes = BaseTypes.lib

    @Deprecated(message = "Please use `Time.lib`.", ReplaceWith("Time.lib"))
    const val time = Time.lib

    @Deprecated(message = "Please use `Time.lib`.", ReplaceWith("Time.lib"))
    const val change = Change.lib

    @Deprecated(message = "Please use `Text.lib`.", ReplaceWith("Text.lib"))
    const val text = Text.lib

    @Deprecated(message = "Please use `TestLib.lib`.", ReplaceWith("TestLib.lib"))
    const val testlib = TestLib.lib

    @Deprecated(message = "Please use `Time.testLib`.", ReplaceWith("Time.testLib"))
    const val testUtilTime = Time.testLib

    @Deprecated(message = "Please use `ToolBase.psiJava` instead`.")
    const val psiJava = "$toolsGroup:spine-psi-java:${ToolBase.version}"

    @Deprecated(message = "Please use `ToolBase.psiJava` instead`.")
    const val psiJavaBundle = "$toolsGroup:spine-psi-java-bundle:${ToolBase.version}"

    @Deprecated(message = "Please use `ToolBase.lib` instead`.")
    const val toolBase = "$toolsGroup:spine-tool-base:${ToolBase.version}"

    @Deprecated(message = "Please use `ToolBase.pluginBase` instead`.")
    const val pluginBase = "$toolsGroup:spine-plugin-base:${ToolBase.version}"

    @Deprecated(
        message = "Please use `ToolBase.pluginTestlib` instead`.",
        ReplaceWith("ToolBase.pluginTestlib")
    )
    const val pluginTestlib = ToolBase.pluginTestlib

    @Deprecated(
        message = "Please use `ModelCompiler.lib` instead.",
        ReplaceWith("ModelCompiler.lib")
    )
    const val modelCompiler = ModelCompiler.lib

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

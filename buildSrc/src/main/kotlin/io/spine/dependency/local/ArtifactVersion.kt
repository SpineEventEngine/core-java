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
 * Versions for published Spine SDK artifacts.
 */
@Suppress("ConstPropertyName")
object ArtifactVersion {

    /**
     * The version of [Spine.base].
     *
     * @see <a href="https://github.com/SpineEventEngine/base">spine-base</a>
     */
    @Deprecated(message = "Please use `Base.version`.", ReplaceWith("Base.version"))
    const val base = Base.version

    @Suppress("unused")
    @Deprecated(
        message = "Please use `Base.versionForBuildScript`.",
        ReplaceWith("Base.versionForBuildScript")
    )
    const val baseForBuildScript = Base.versionForBuildScript

    /**
     * The version of [Spine.reflect].
     *
     * @see <a href="https://github.com/SpineEventEngine/reflect">spine-reflect</a>
     */
    @Deprecated(message = "Please use `Reflect.version`.", ReplaceWith("Reflect.version"))
    const val reflect = Reflect.version

    /**
     * The version of [Logging].
     */
    @Deprecated(message = "Please use `Logging.version`.", ReplaceWith("Logging.version"))
    const val logging = Logging.version

    /**
     * The version of [Spine.testlib].
     *
     * @see <a href="https://github.com/SpineEventEngine/testlib">spine-testlib</a>
     */
    @Deprecated(message = "Please use `TestLib.version`.", ReplaceWith("TestLib.version"))
    const val testlib = TestLib.version

    /**
     * The version of `core-java`.
     */
    @Deprecated(message = "Please use `CoreJava.version`.", ReplaceWith("CoreJava.version"))
    const val core = CoreJava.version

    /**
     * The version of [Spine.modelCompiler].
     *
     * @see <a href="https://github.com/SpineEventEngine/model-compiler">spine-model-compiler</a>
     */
    @Suppress("unused")
    @Deprecated(
        message = "Please use `ModelCompiler.version` instead.",
        ReplaceWith("ModelCompiler.version")
    )
    const val mc = ModelCompiler.version

    /**
     * The version of [Spine.baseTypes].
     *
     * @see <a href="https://github.com/SpineEventEngine/base-types">spine-base-types</a>
     */
    @Deprecated(message = "Please use `BaseTypes.version`.", ReplaceWith("BaseTypes.version"))
    const val baseTypes = BaseTypes.version

    /**
     * The version of [Spine.time].
     *
     * @see <a href="https://github.com/SpineEventEngine/time">spine-time</a>
     */
    @Deprecated(message = "Please use `Time.version`.", ReplaceWith("Time.version"))
    const val time = Time.version

    /**
     * The version of [Spine.change].
     *
     * @see <a href="https://github.com/SpineEventEngine/change">spine-change</a>
     */
    @Deprecated(message = "Please use `Change.version`.", ReplaceWith("Change.version"))
    const val change = Change.version

    /**
     * The version of [Spine.text].
     *
     * @see <a href="https://github.com/SpineEventEngine/text">spine-text</a>
     */
    @Deprecated(message = "Please use `Text.version`.", ReplaceWith("Text.version"))
    const val text = Text.version

    /**
     * The version of [Spine.toolBase].
     *
     * @see <a href="https://github.com/SpineEventEngine/tool-base">spine-tool-base</a>
     */
    @Suppress("unused")
    @Deprecated(message = "Please use `ToolBase.version`.", ReplaceWith("ToolBase.version"))
    const val toolBase = ToolBase.version

    /**
     * The version of [Spine.javadocFilter].
     *
     * @see <a href="https://github.com/SpineEventEngine/doc-tools">spine-javadoc-tools</a>
     */
    const val javadocTools = "2.0.0-SNAPSHOT.75"
}

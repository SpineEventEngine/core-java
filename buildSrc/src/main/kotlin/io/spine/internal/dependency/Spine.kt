/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.internal.dependency

/**
 * Dependencies on Spine modules.
 */
@Suppress("unused", "ConstPropertyName")
object Spine {

    const val group = "io.spine"
    const val toolsGroup = "io.spine.tools"

    /**
     * Versions for published Spine SDK artifacts.
     */
    object ArtifactVersion {

        /**
         * The version of [Spine.base].
         *
         * @see <a href="https://github.com/SpineEventEngine/base">spine-base</a>
         */
        const val base = "2.0.0-SNAPSHOT.195"

        /**
         * The version of [Spine.reflect].
         *
         * @see <a href="https://github.com/SpineEventEngine/reflect">spine-reflect</a>
         */
        const val reflect = "2.0.0-SNAPSHOT.183"

        /**
         * The version of [Spine.logging].
         *
         * @see <a href="https://github.com/SpineEventEngine/logging">spine-logging</a>
         */
        const val logging = "2.0.0-SNAPSHOT.233"

        /**
         * The version of [Spine.testlib].
         *
         * @see <a href="https://github.com/SpineEventEngine/testlib">spine-testlib</a>
         */
        const val testlib = "2.0.0-SNAPSHOT.184"

        /**
         * The version of `core-java`.
         *
         * @see [Spine.CoreJava.client]
         * @see [Spine.CoreJava.server]
         * @see <a href="https://github.com/SpineEventEngine/core-java">core-java</a>
         */
        const val core = "2.0.0-SNAPSHOT.175"

        /**
         * The version of [Spine.modelCompiler].
         *
         * @see <a href="https://github.com/SpineEventEngine/model-compiler">spine-model-compiler</a>
         */
        const val mc = "2.0.0-SNAPSHOT.133"

        /**
         * The version of [McJava].
         *
         * @see <a href="https://github.com/SpineEventEngine/mc-java">spine-mc-java</a>
         */
        const val mcJava = "2.0.0-SNAPSHOT.177"

        /**
         * The version of [Spine.baseTypes].
         *
         * @see <a href="https://github.com/SpineEventEngine/base-types">spine-base-types</a>
         */
        const val baseTypes = "2.0.0-SNAPSHOT.126"

        /**
         * The version of [Spine.time].
         *
         * @see <a href="https://github.com/SpineEventEngine/time">spine-time</a>
         */
        const val time = "2.0.0-SNAPSHOT.135"

        /**
         * The version of [Spine.change].
         *
         * @see <a href="https://github.com/SpineEventEngine/change">spine-change</a>
         */
        const val change = "2.0.0-SNAPSHOT.118"

        /**
         * The version of [Spine.text].
         *
         * @see <a href="https://github.com/SpineEventEngine/text">spine-text</a>
         */
        const val text = "2.0.0-SNAPSHOT.6"

        /**
         * The version of [Spine.toolBase].
         *
         * @see <a href="https://github.com/SpineEventEngine/tool-base">spine-tool-base</a>
         */
        const val toolBase = "2.0.0-SNAPSHOT.193"

        /**
         * The version of [Spine.javadocTools].
         *
         * @see <a href="https://github.com/SpineEventEngine/doc-tools">spine-javadoc-tools</a>
         */
        const val javadocTools = "2.0.0-SNAPSHOT.75"
    }

    const val base = "$group:spine-base:${ArtifactVersion.base}"

    @Deprecated("Use `Logging.lib` instead.", ReplaceWith("Logging.lib"))
    const val logging = "$group:spine-logging:${ArtifactVersion.logging}"
    @Deprecated("Use `Logging.context` instead.", ReplaceWith("Logging.context"))
    const val loggingContext = "$group:spine-logging-context:${ArtifactVersion.logging}"
    @Deprecated("Use `Logging.backend` instead.", ReplaceWith("Logging.backend"))
    const val loggingBackend = "$group:spine-logging-backend:${ArtifactVersion.logging}"

    const val reflect = "$group:spine-reflect:${ArtifactVersion.reflect}"
    const val baseTypes = "$group:spine-base-types:${ArtifactVersion.baseTypes}"
    const val time = "$group:spine-time:${ArtifactVersion.time}"
    const val change = "$group:spine-change:${ArtifactVersion.change}"
    const val text = "$group:spine-text:${ArtifactVersion.text}"

    const val testlib = "$toolsGroup:spine-testlib:${ArtifactVersion.testlib}"
    const val testUtilTime = "$toolsGroup:spine-testutil-time:${ArtifactVersion.time}"
    const val toolBase = "$toolsGroup:spine-tool-base:${ArtifactVersion.toolBase}"
    const val pluginBase = "$toolsGroup:spine-plugin-base:${ArtifactVersion.toolBase}"
    const val pluginTestlib = "$toolsGroup:spine-plugin-testlib:${ArtifactVersion.toolBase}"
    const val modelCompiler = "$toolsGroup:spine-model-compiler:${ArtifactVersion.mc}"

    /**
     * Dependencies on the artifacts of the Spine Logging library.
     *
     * @see <a href="https://github.com/SpineEventEngine/logging">spine-logging</a>
     */
    object Logging {
        const val version = ArtifactVersion.logging
        const val lib = "$group:spine-logging:$version"

        const val log4j2Backend = "$group:spine-logging-log4j2-backend:$version"
        const val stdContext = "$group:spine-logging-std-context:$version"
        const val grpcContext = "$group:spine-logging-grpc-context:$version"
        const val smokeTest = "$group:spine-logging-smoke-test:$version"

        // Transitive dependencies.
        // Make `public` and use them to force a version in a particular repository, if needed.
        internal const val julBackend = "$group:spine-logging-jul-backend:$version"
        internal const val middleware = "$group:spine-logging-middleware:$version"
        internal const val platformGenerator = "$group:spine-logging-platform-generator:$version"
        internal const val jvmDefaultPlatform = "$group:spine-logging-jvm-default-platform:$version"

        @Deprecated(
            message = "Please use `Logging.lib` instead.",
            replaceWith = ReplaceWith("lib")
        )
        const val floggerApi = "$group:spine-flogger-api:$version"

        @Deprecated(
            message = "Please use `grpcContext` instead.",
            replaceWith = ReplaceWith("grpcContext")
        )
        const val floggerGrpcContext = "$group:spine-flogger-grpc-context:$version"
    }

    /**
     * Dependencies on Spine Model Compiler for Java.
     *
     * See [mc-java](https://github.com/SpineEventEngine/mc-java).
     */
    @Suppress("MemberVisibilityCanBePrivate") // `pluginLib()` is used by subprojects.
    object McJava {
        const val version = ArtifactVersion.mcJava
        const val pluginId = "io.spine.mc-java"
        val pluginLib = pluginLib(version)
        fun pluginLib(version: String): String = "$toolsGroup:spine-mc-java-plugins:$version:all"
    }

    @Deprecated("Please use `javadocFilter` instead.", ReplaceWith("javadocFilter"))
    const val javadocTools = "$toolsGroup::${ArtifactVersion.javadocTools}"
    const val javadocFilter = "$toolsGroup:spine-javadoc-filter:${ArtifactVersion.javadocTools}"

    const val client = CoreJava.client // Added for brevity.
    const val server = CoreJava.server // Added for brevity.

    /**
     * Dependencies on `core-java` modules.
     *
     * See [`SpineEventEngine/core-java`](https://github.com/SpineEventEngine/core-java/).
     */
    object CoreJava {
        const val version = ArtifactVersion.core
        const val core = "$group:spine-core:$version"
        const val client = "$group:spine-client:$version"
        const val server = "$group:spine-server:$version"
        const val testUtilServer = "$toolsGroup:spine-testutil-server:$version"
    }
}

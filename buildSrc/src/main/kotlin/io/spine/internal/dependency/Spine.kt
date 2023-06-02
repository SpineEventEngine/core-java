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

package io.spine.internal.dependency

/**
 * Dependencies on Spine modules.
 */
@Suppress("unused")
object Spine {

    const val group = "io.spine"
    const val toolsGroup = "io.spine.tools"

    /**
     * Versions for published Spine SDK artifacts.
     */
    object ArtifactVersion {

        /** The version of [ProtoData]. */
        @Deprecated("Please use `ProtoData.version` instead.")
        const val protoData = ProtoData.version

        /** The version of [Spine.base]. */
        const val base = "2.0.0-SNAPSHOT.180"

        /** The version of [Spine.reflect]. */
        const val reflect = "2.0.0-SNAPSHOT.182"

        /** The version of [Spine.logging]. */
        const val logging = "2.0.0-SNAPSHOT.186"

        /** The version of [Spine.testlib]. */
        const val testlib = "2.0.0-SNAPSHOT.183"

        /**
         * The version of `core-java`.
         * @see [Spine.CoreJava.client]
         * @see [Spine.CoreJava.server]
         */
        const val core = "2.0.0-SNAPSHOT.141"

        /** The version of [Spine.modelCompiler]. */
        const val mc = "2.0.0-SNAPSHOT.130"

        /** The version of [McJava]. */
        const val mcJava = "2.0.0-SNAPSHOT.147"

        /** The version of [Spine.baseTypes]. */
        const val baseTypes = "2.0.0-SNAPSHOT.121"

        /** The version of [Spine.time]. */
        const val time = "2.0.0-SNAPSHOT.121"

        /** The version of [Spine.change]. */
        const val change = "2.0.0-SNAPSHOT.118"

        /** The version of [Spine.text]. */
        const val text = "2.0.0-SNAPSHOT.3"

        /** The version of [Spine.toolBase]. */
        const val toolBase = "2.0.0-SNAPSHOT.156"

        /** The version of [Spine.validation]. */
        @Deprecated("Please use `Validation.version` instead.")
        const val validation = Validation.version

        /** The version of [Spine.javadocTools]. */
        const val javadocTools = "2.0.0-SNAPSHOT.75"
    }

    /** The version of ProtoData to be used in the project. */
    @Deprecated("Please use `ProtoData.version` instead.")
    const val protoDataVersion = ProtoData.version

    const val base = "$group:spine-base:${ArtifactVersion.base}"
    const val logging = "$group:spine-logging:${ArtifactVersion.logging}"
    const val loggingContext = "$group:spine-logging-context:${ArtifactVersion.logging}"
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

    object McJava {
        const val version = ArtifactVersion.mcJava
        const val pluginId = "io.spine.mc-java"
        const val pluginLib = "$toolsGroup:spine-mc-java-plugins:${version}:all"
    }

    const val javadocTools = "$toolsGroup::${ArtifactVersion.javadocTools}"

    @Deprecated("Please use `validation.runtime`", replaceWith = ReplaceWith("validation.runtime"))
    const val validate = "$group:spine-validate:${ArtifactVersion.base}"

    @Deprecated("Please use `Validation` instead.")
    val validation = Validation

    @Suppress("MemberVisibilityCanBePrivate")
    @Deprecated("Please use `CoreJava` instead.")
    val coreJava = CoreJava

    const val client = CoreJava.client // Added for brevity.
    const val server = CoreJava.server // Added for brevity.

    /**
     * Dependencies on `core-java` modules.
     *
     * See [`SpineEventEngine/core-java`](https://github.com/SpineEventEngine/core-java/).
     */
    object CoreJava {
        const val core = "$group:spine-core:${ArtifactVersion.core}"
        const val client = "$group:spine-client:${ArtifactVersion.core}"
        const val server = "$group:spine-server:${ArtifactVersion.core}"
        const val testUtilServer = "$toolsGroup:spine-testutil-server:${ArtifactVersion.core}"
    }
}

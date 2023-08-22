/*
 * Copyright 2023, TeamDev. All rights reserved.
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

// https://github.com/Kotlin/dokka
@Suppress("unused", "ConstPropertyName")
object Dokka {
    private const val group = "org.jetbrains.dokka"

    /**
     * When changing the version, also change the version used in the
     * `buildSrc/build.gradle.kts`.
     */
    const val version = "1.8.20"

    object GradlePlugin {
        const val id = "org.jetbrains.dokka"

        /**
         * The version of this plugin is already specified in `buildSrc/build.gradle.kts`
         * file. Thus, when applying the plugin in project's build files, only the [id]
         * should be used.
         */
        const val lib = "${group}:dokka-gradle-plugin:${version}"
    }

    object BasePlugin {
        const val lib = "${group}:dokka-base:${version}"
    }

    const val analysis = "org.jetbrains.dokka:dokka-analysis:${version}"

    object CorePlugin {
        const val lib = "${group}:dokka-core:${version}"
    }

    /**
     * To generate the documentation as seen from Java perspective use this plugin.
     *
     * @see <a href="https://github.com/Kotlin/dokka#output-formats">
     *     Dokka output formats</a>
     */
    object KotlinAsJavaPlugin {
        const val lib = "${group}:kotlin-as-java-plugin:${version}"
    }

    /**
     * Custom Dokka plugins developed for Spine-specific needs like excluding by
     * `@Internal` annotation.
     *
     * @see <a href="https://github.com/SpineEventEngine/dokka-tools/tree/master/dokka-extensions">
     *     Custom Dokka Plugins</a>
     */
    object SpineExtensions {
        private const val group = "io.spine.tools"

        const val version = "2.0.0-SNAPSHOT.4"
        const val lib = "${group}:spine-dokka-extensions:${version}"
    }
}

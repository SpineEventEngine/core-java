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

import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.extra

/**
 * Dependencies on Spine modules.
 *
 * @constructor
 * Creates a new instance of `Spine` taking the property values
 * of versions from the given project's extra properties.
 */
@Suppress("unused")
class Spine(p: ExtensionAware) {

    /**
     * Default versions for the modules of Spine, unless they are
     * configured in `versions.gradle.kts`.
     */
    object DefaultVersion {

        /**
         * The default version  of `base` to use.
         * @see [Spine.base]
         */
        const val base = "2.0.0-SNAPSHOT.112"

        /**
         * The version of `model-compiler` to use.
         * @see [Spine.modelCompiler]
         */
        const val mc = "2.0.0-SNAPSHOT.90"

        /**
         * The version of `mc-java` to use.
         */
        const val mcJava = "2.0.0-SNAPSHOT.102"

        /**
         * The version of `base-types` to use.
         * @see [Spine.baseTypes]
         */
        const val baseTypes = "2.0.0-SNAPSHOT.108"

        /**
         * The version of `time` to use.
         * @see [Spine.time]
         */
        const val time = "2.0.0-SNAPSHOT.108"

        /**
         * The version of `tool-base` to use.
         * @see [Spine.toolBase]
         */
        const val toolBase = "2.0.0-SNAPSHOT.109"

        /**
         * The version of `validation` to use.
         * @see [Spine.validation]
         */
        const val validation = "2.0.0-SNAPSHOT.32"
    }

    companion object {
        const val group = "io.spine"
        const val toolsGroup = "io.spine.tools"

        /**
         * The version of ProtoData to be used in the project.
         *
         * We do it here instead of `versions.gradle.kts` because we later use
         * it in a `plugins` section in a build script.
         *
         * @see [ProtoData]
         */
        const val protoDataVersion = "0.2.18"
    }

    val base = "$group:spine-base:${p.baseVersion}"
    val testlib = "$toolsGroup:spine-testlib:${p.baseVersion}"

    @Deprecated("Please use `validation.runtime`", replaceWith = ReplaceWith("validation.runtime"))
    val validate = "$group:spine-validate:${p.baseVersion}"

    val baseTypes = "$group:spine-base-types:${p.baseTypesVersion}"

    val time = "$toolsGroup:spine-testlib:${p.timeVersion}"

    val toolBase = "$toolsGroup:spine-tool-base:${p.toolBaseVersion}"
    val pluginBase = "$toolsGroup:spine-plugin-base:${p.toolBaseVersion}"
    val pluginTestlib = "$toolsGroup:spine-plugin-testlib:${p.toolBaseVersion}"

    val modelCompiler = "$toolsGroup:spine-model-compiler:${p.mcVersion}"

    val mcJavaPlugin = "io.spine.tools:spine-mc-java-plugins:${p.mcJavaVersion}:all"

    val validation = Validation(p)

    private val ExtensionAware.baseVersion: String
        get() = "baseVersion".asExtra(this, DefaultVersion.base)

    private val ExtensionAware.baseTypesVersion: String
        get() = "baseTypesVersion".asExtra(this, DefaultVersion.baseTypes)

    private val ExtensionAware.timeVersion: String
        get() = "timeVersion".asExtra(this, DefaultVersion.time)

    private val ExtensionAware.mcVersion: String
        get() = "mcVersion".asExtra(this, DefaultVersion.mc)

    private val ExtensionAware.mcJavaVersion: String
        get() = "mcJavaVersion".asExtra(this, DefaultVersion.mcJava)

    private val ExtensionAware.toolBaseVersion: String
        get() = "toolBaseVersion".asExtra(this, DefaultVersion.toolBase)

    /**
     * Dependencies on Spine validation modules.
     *
     * See [`SpineEventEngine/validation`](https://github.com/SpineEventEngine/validation/).
     */
    class Validation(p: ExtensionAware) {

        companion object {
            const val group = "io.spine.validation"
        }

        val runtime = "$group:spine-validation-java-runtime:${p.validationVersion}"
        val java = "$group:spine-validation-java:${p.validationVersion}"
        val model = "$group:spine-validation-model:${p.validationVersion}"
        val config = "$group:spine-validation-configuration:${p.validationVersion}"

        private val ExtensionAware.validationVersion: String
            get() = "validationVersion".asExtra(this, DefaultVersion.validation)
    }

    /**
     * Dependencies on ProtoData modules.
     *
     * See [`SpineEventEngine/ProtoData`](https://github.com/SpineEventEngine/ProtoData/).
     */
    object ProtoData {

        const val pluginId = "io.spine.protodata"

        const val version = protoDataVersion
        const val pluginLib = "$group:protodata:$version"
    }
}

/**
 * Obtains the value of the extension property named as this string from the given project.
 *
 * @param p the project declaring extension properties
 * @param defaultValue
 *         the default value to return, if the project does not have such a property.
 *         If `null` then rely on the property declaration, even if this would cause an error.
 */
private fun String.asExtra(p: ExtensionAware, defaultValue: String? = null): String {
    return if (p.extra.has(this) || defaultValue == null) {
        p.extra[this] as String
    } else {
        defaultValue
    }
}

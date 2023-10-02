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

/**
 * Dependencies on ProtoData modules.
 *
 * In order to use locally published ProtoData version instead of the version from a public plugin
 * registry, set the `PROTO_DATA_VERSION` and/or the `PROTO_DATA_DF_VERSION` environment variables
 * and stop the Gradle daemons so that Gradle observes the env change:
 * ```
 * export PROTO_DATA_VERSION=0.43.0-local
 * export PROTO_DATA_DF_VERSION=0.41.0
 *
 * ./gradle --stop
 * ./gradle build   # Conduct the intended checks.
 * ```
 *
 * Then, in order to reset the console to run the usual versions again, remove the values of
 * the environment variables and stop the daemon:
 * ```
 * export PROTO_DATA_VERSION=""
 * export PROTO_DATA_DF_VERSION=""
 *
 * ./gradle --stop
 * ```
 *
 * See [`SpineEventEngine/ProtoData`](https://github.com/SpineEventEngine/ProtoData/).
 */
@Suppress("unused", "ConstPropertyName")
object ProtoData {
    const val group = "io.spine.protodata"
    const val pluginId = "io.spine.protodata"

    /**
     * The version of ProtoData dependencies.
     */
    val version: String
    private const val fallbackVersion = "0.11.0"

    /**
     * The distinct version of ProtoData used by other build tools.
     *
     * When ProtoData is used both for building the project and as a part of the Project's
     * transitional dependencies, this is the version used to build the project itself.
     */
    val dogfoodingVersion: String
    private const val fallbackDfVersion = "0.9.11"

    /**
     * The artifact for the ProtoData Gradle plugin.
     */
    val pluginLib: String

    val compiler
        get() = "$group:protodata-compiler:$version"
    val codegenJava
        get() = "$group:protodata-codegen-java:$version"

    /**
     * An env variable storing a custom [version].
     */
    private const val VERSION_ENV = "PROTO_DATA_VERSION"

    /**
     * An env variable storing a custom [dogfoodingVersion].
     */
    private const val DF_VERSION_ENV = "PROTO_DATA_DF_VERSION"

    /**
     * Sets up the versions and artifacts for the build to use.
     *
     * If either [VERSION_ENV] or [DF_VERSION_ENV] is set, those versions are used instead of
     * the hardcoded ones. Also, in this mode, the [pluginLib] coordinates are changed so that
     * it points at a locally published artifact. Otherwise it points at an artifact that would be
     * published to a public plugin registry.
     */
    init {
        val experimentVersion = System.getenv(VERSION_ENV)
        val experimentDfVersion = System.getenv(DF_VERSION_ENV)
        if (experimentVersion?.isNotBlank() == true || experimentDfVersion?.isNotBlank() == true) {
            version = experimentVersion ?: fallbackVersion
            dogfoodingVersion = experimentDfVersion ?: fallbackDfVersion

            pluginLib = "${group}:gradle-plugin:$version"
            println("""

                ❗ Running an experiment with ProtoData. ❗
                -----------------------------------------
                    Regular version     = v$version
                    Dogfooding version  = v$dogfoodingVersion
                
                    ProtoData Gradle plugin can now be loaded from Maven Local.
                    
                    To reset the versions, erase the `$$VERSION_ENV` and `$$DF_VERSION_ENV` environment variables. 

            """.trimIndent())
        } else {
            version = fallbackVersion
            dogfoodingVersion = fallbackDfVersion
            pluginLib = "${Spine.group}:protodata:$version"
        }
    }
}

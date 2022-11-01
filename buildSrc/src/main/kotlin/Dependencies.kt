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

@file:Suppress("unused", "UnusedReceiverParameter")

import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.GradleDoctor
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Spine.ProtoData
import org.gradle.plugin.use.PluginDependenciesSpec

/**
 * Allows to escape a fully qualified names for dependencies which cannot be used
 * under `plugins` section because `io` is a value declared in
 * `org.gradle.kotlin.dsl.PluginAccessors.kt`.
 *
 * We still want to keep versions numbers in [io.spine.internal.dependency.Spine]
 * for the time being. So this file allows to reference those without resorting
 * to using strings again.
 */
private const val ABOUT = ""

val PluginDependenciesSpec.protoData: ProtoData
    get() = ProtoData

val PluginDependenciesSpec.errorPronePlugin: String
    get() = ErrorProne.GradlePlugin.id

val PluginDependenciesSpec.protobufPlugin: String
    get() = Protobuf.GradlePlugin.id

val PluginDependenciesSpec.gradleDoctor: GradleDoctor
    get() = GradleDoctor

val PluginDependenciesSpec.mcJava: Spine.McJava
    get() = Spine.McJava

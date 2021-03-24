/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import io.spine.gradle.internal.Deps
import io.spine.gradle.internal.IncrementGuard

val spineBaseVersion: String by extra
val spineTimeVersion: String by extra

dependencies {
    api("io.spine:spine-base:$spineBaseVersion")
    api("io.spine:spine-time:$spineTimeVersion")

    testImplementation(project(":testutil-core"))
    testImplementation("io.spine.tools:spine-testutil-time:$spineTimeVersion")
}

modelCompiler {
    fields {

        // Describe the `Event` fields to allow non-reflective and strongly-typed access.
        generateFor("spine.core.Event", markAs("io.spine.core.EventField"))

        // Enable the strongly-typed fields generation for `spine.core.EventContext` to allow
        // creation of typed event filters based on event context.
        generateFor("spine.core.EventContext", markAs("io.spine.core.EventContextField"))
    }
}

apply {
    with(Deps.scripts) {
        from(testArtifacts(project))
        from(publishProto(project))
    }
    plugin(IncrementGuard::class)
}

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

package io.spine.dependency.lib

// https://github.com/grpc/grpc-java
@Suppress("unused", "ConstPropertyName")
object Grpc {
    @Suppress("MemberVisibilityCanBePrivate")
    const val version        = "1.59.0"
    const val group          = "io.grpc"
    const val api            = "$group:grpc-api:$version"
    const val auth           = "$group:grpc-auth:$version"
    const val core           = "$group:grpc-core:$version"
    const val context        = "$group:grpc-context:$version"
    const val inProcess      = "$group:grpc-inprocess:$version"
    const val stub           = "$group:grpc-stub:$version"
    const val okHttp         = "$group:grpc-okhttp:$version"
    const val protobuf       = "$group:grpc-protobuf:$version"
    const val protobufLite   = "$group:grpc-protobuf-lite:$version"
    const val netty          = "$group:grpc-netty:$version"
    const val nettyShaded    = "$group:grpc-netty-shaded:$version"

    object ProtocPlugin {
        const val id = "grpc"
        const val artifact = "$group:protoc-gen-grpc-java:$version"
    }
}

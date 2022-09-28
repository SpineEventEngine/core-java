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

// https://github.com/grpc/grpc-java
@Suppress("unused")
object Grpc {
    @Suppress("MemberVisibilityCanBePrivate")
    const val version        = "1.46.0"
    const val api            = "io.grpc:grpc-api:${version}"
    const val auth           = "io.grpc:grpc-auth:${version}"
    const val core           = "io.grpc:grpc-core:${version}"
    const val context        = "io.grpc:grpc-context:${version}"
    const val stub           = "io.grpc:grpc-stub:${version}"
    const val okHttp         = "io.grpc:grpc-okhttp:${version}"
    const val protobuf       = "io.grpc:grpc-protobuf:${version}"
    const val protobufLite   = "io.grpc:grpc-protobuf-lite:${version}"
    const val protobufPlugin = "io.grpc:protoc-gen-grpc-java:${version}"
    const val netty          = "io.grpc:grpc-netty:${version}"
    const val nettyShaded    = "io.grpc:grpc-netty-shaded:${version}"
}

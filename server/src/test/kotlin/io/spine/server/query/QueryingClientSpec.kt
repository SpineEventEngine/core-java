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

package io.spine.server.query

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth8.assertThat
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
//import io.spine.protodata.CodeGenerationContext
//import io.spine.protodata.FilePath
//import io.spine.protodata.ProtobufCompilerContext
//import io.spine.protodata.ProtobufSourceFile
//import io.spine.protodata.events.CompilerEvents
//import io.spine.protodata.path
//import io.spine.protodata.test.DoctorProto
//import io.spine.protodata.test.ProjectProto
import io.spine.server.BoundedContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`QueryingClient` should")
class QueryingClientSpec {

//    private val fileNames: Correspondence<ProtobufSourceFile, String> = Correspondence.from(
//        { file, path -> file!!.file.path.value == path },
//        "file name"
//    )
//
//    private val actor = QueryingClientSpec::class.simpleName!!
//    private lateinit var context: BoundedContext
//
//    private val files = listOf(DoctorProto.getDescriptor(), ProjectProto.getDescriptor())
//
    @BeforeEach
    fun initContext() {
//        context = CodeGenerationContext.builder().build()
//        val request = CodeGeneratorRequest.newBuilder()
//            .addAllProtoFile(files.map { it.toProto() })
//            .addAllFileToGenerate(files.map { it.name })
//            .build()
//        val events = CompilerEvents.parse(request)
//        ProtobufCompilerContext().use {
//            it.emitted(events)
//        }
    }
//
    @AfterEach
    fun shutDown() {
//        context.close()
    }

    @Test
    fun `fetch multiple results`() {
//        val client = QueryingClient(context, ProtobufSourceFile::class.java, actor)
//        val sources = client.all()
//        val assertSources = assertThat(sources)
//        assertSources
//            .hasSize(2)
//        assertSources
//            .comparingElementsUsing(fileNames)
//            .containsExactlyElementsIn(files.map { it.name })
    }

    @Test
    fun `fetch the only one`() {
//        val client = QueryingClient(context, ProtobufSourceFile::class.java, actor)
//        val path = files.first().path()
//        val source = client.withId(path)
//        assertThat(source)
//            .isPresent()
//        assertThat(source.get().file.path)
//            .isEqualTo(path)
    }

    @Test
    fun `fetch none`() {
//        val client = QueryingClient(context, ProtobufSourceFile::class.java, actor)
//        val path = FilePath.newBuilder()
//            .setValue("non/existent/path.proto")
//            .build()
//        val source = client.withId(path)
//        assertThat(source)
//            .isEmpty()
    }
}

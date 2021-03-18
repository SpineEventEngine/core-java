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

package io.spine.server.storage.memory.given;

import com.google.protobuf.Any;
import io.spine.protobuf.AnyPacker;
import io.spine.query.RecordColumn;
import io.spine.query.RecordQuery;
import io.spine.query.Subject;
import io.spine.server.entity.EntityRecord;
import io.spine.test.storage.StgProject;
import io.spine.testdata.Sample;

import static io.spine.query.RecordColumn.create;

/**
 * The test environment for {@link io.spine.server.storage.memory.RecordQueryMatcher} tests.
 *
 * <p>Provides various types of {@linkplain RecordColumn record columns}
 * that can be used to emulate a client-side query.
 */
@SuppressWarnings("BadImport")       // `create` looks fine in this context.
public final class RecordQueryMatcherTestEnv {

    /** Prevents instantiation of this test env class. */
    private RecordQueryMatcherTestEnv() {
    }

    /**
     * Creates an empty {@code Subject} for the {@link EntityRecord}.
     */
    public static Subject<Object, EntityRecord> recordSubject() {
        return RecordQuery.newBuilder(Object.class, EntityRecord.class)
                          .build()
                          .subject();
    }

    /**
     * Creates a {@code Subject} for the {@link EntityRecord} with the given ID.
     */
    public static <I> Subject<I, EntityRecord> recordSubject(I id) {
        return RecordQuery.newBuilder(parameterizedClsOf(id), EntityRecord.class)
                          .id().is(id)
                          .build()
                          .subject();
    }

    @SuppressWarnings("unchecked")  // as per the declaration.
    private static <I> Class<I> parameterizedClsOf(I id) {
        return (Class<I>) id.getClass();
    }

    /**
     * A {@code Column} which holds an {@link Any} instance.
     */
    public static RecordColumn<EntityRecord, Any> anyColumn() {
        return create("wrapped_state", Any.class, (r) -> anyValue());
    }

    /**
     * The {@link Any} value held by the corresponding {@linkplain #anyColumn() entity column}.
     */
    public static Any anyValue() {
        StgProject someMessage = Sample.messageOfType(StgProject.class);
        Any value = AnyPacker.pack(someMessage);
        return value;
    }

    /**
     * A {@code Column} which holds a {@code boolean} value.
     */
    public static RecordColumn<EntityRecord, Boolean> booleanColumn() {
        return create("internal", Boolean.class, (r) -> booleanValue());
    }

    /**
     * A {@code Column} which holds a {@code boolean} value.
     */
    public static RecordColumn<EntityRecord, Boolean> booleanColumn(String name) {
        return create(name, Boolean.class, (r) -> booleanValue());
    }

    /**
     * The {@code boolean} value held by the corresponding {@linkplain #booleanColumn() entity
     * column}.
     */
    private static boolean booleanValue() {
        return true;
    }
}

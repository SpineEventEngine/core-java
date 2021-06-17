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

package io.spine.server.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import io.spine.query.Column;
import io.spine.query.RecordColumn;
import io.spine.server.storage.given.StgColumn;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;

@DisplayName("`MessageRecordSpec` should")
@SuppressWarnings("DuplicateStringLiteralInspection")   /* Similar tests have similar names. */
class MessageRecordSpecTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicInstanceMethods(spec());
    }

    @Test
    @DisplayName("obtain a column by name")
    void obtainByName() {
        MessageRecordSpec<StgProjectId, StgProject> spec = spec();
        StgColumn.definitions()
                 .forEach(column -> assertColumn(spec, column));
    }

    @Test
    @DisplayName("return all definitions of the columns")
    void returnAllColumns() {
        ImmutableSet<Column<?, ?>> actualColumns = spec().columns();
        assertThat(actualColumns).containsExactlyElementsIn(StgColumn.definitions());
    }

    private static MessageRecordSpec<StgProjectId, StgProject> spec() {
        return new MessageRecordSpec<>(StgProjectId.class, StgProject.class,
                                       StgProject::getId, StgColumn.definitions());
    }

    private static void assertColumn(
            MessageRecordSpec<StgProjectId, StgProject> spec, RecordColumn<StgProject, ?> column) {
        Optional<Column<?, ?>> found = spec.findColumn(column.name());
        assertThat(found).isPresent();
        assertThat(found.get()
                        .type()).isEqualTo(column.type());
    }
}

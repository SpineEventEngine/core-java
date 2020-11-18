/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage;

import io.spine.core.Version;
import io.spine.query.ColumnName;
import io.spine.query.CustomColumn;
import io.spine.server.entity.Entity;

/**
 * A column of an entity to store the {@linkplain Entity#version() entity version}.
 */
final class VersionColumn extends CustomColumn<Entity<?, ?>, Version> {

    @SuppressWarnings("DuplicateStringLiteralInspection")   // Used in a different context.
    private static final ColumnName VERSION = ColumnName.of("version");

    VersionColumn() {
        super();
    }

    @Override
    public ColumnName name() {
        return VERSION;
    }

    @Override
    public Class<Version> type() {
        return Version.class;
    }

    @Override
    public Version valueIn(Entity<?, ?> entity) {
        return entity.version();
    }
}
/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.errorprone.annotations.Immutable;
import io.spine.annotation.SPI;

/**
 * A column of the {@linkplain io.spine.server.entity.Entity entity}.
 *
 * <p>Columns are the entity state fields which are stored separately from the entity record and
 * can be used as criteria for target {@linkplain io.spine.client.Filter filters} during the entity
 * querying.
 *
 * <p>The {@linkplain #name() name} of the column represents the value which needs to be specified
 * to the filter. The {@linkplain #type() type} is an expected type of the filter value.
 */
@SPI
@Immutable
public interface Column {

    /**
     * The name of the column in the storage.
     */
    ColumnName name();

    /**
     * The type of the column.
     *
     * <p>As user-defined columns are proto-based, there is a fixed set of possible column types.
     * See {@link ColumnMapping}.
     */
    Class<?> type();
}

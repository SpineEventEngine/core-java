/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity.storage;

import com.google.common.collect.ImmutableMap;
import org.spine3.client.AggregatingColumnFilter.AggregatingOperator;
import org.spine3.client.ColumnFilter;

import java.io.Serializable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.copyOf;

/**
 * @author Dmytro Dashenkov
 */
public final class AggregatingQueryParameter implements Serializable {

    private static final long serialVersionUID = -475685759190562528L;

    private final AggregatingOperator operator;

    private final ImmutableMap<Column, ColumnFilter> filters;

    AggregatingQueryParameter(AggregatingOperator operator, Map<Column, ColumnFilter> filters) {
        this.operator = checkNotNull(operator);
        this.filters = copyOf(filters);
    }

    public AggregatingOperator getOperator() {
        return operator;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField") // Immutable structure
    public ImmutableMap<Column, ColumnFilter> getFilters() {
        return filters;
    }
}

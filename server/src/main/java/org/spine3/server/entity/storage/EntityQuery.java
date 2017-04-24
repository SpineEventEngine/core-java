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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.spine3.client.EntityIdFilter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmytro Dashenkov
 */
public final class EntityQuery {

    private final EntityIdFilter idFilter;
    private final ImmutableMultimap<Column<?>, Object> parameters;

    static EntityQuery of(EntityIdFilter idFilter, Multimap<Column<?>, Object> parameters) {
        checkNotNull(idFilter);
        checkNotNull(parameters);
        return new EntityQuery(idFilter, parameters);
    }

    private EntityQuery(EntityIdFilter idFilter, Multimap<Column<?>, Object> parameters) {
        this.idFilter = idFilter;
        this.parameters = ImmutableMultimap.copyOf(parameters);
    }

    public Multimap<Column<?>, Object> getParameters() {
        return parameters;
    }

    public EntityIdFilter getIdFilter() {
        return idFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityQuery that = (EntityQuery) o;
        return Objects.equal(idFilter, that.idFilter) &&
                Objects.equal(getParameters(), that.getParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(idFilter, getParameters());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("idFilter", idFilter)
                          .add("parameters", parameters)
                          .toString();
    }
}

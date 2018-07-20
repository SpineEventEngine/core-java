/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity;

import java.util.Objects;

/**
 * Default factory that creates entities by invoking constructor that
 * accepts a single ID parameter.
 *
 * @param <I> the type of entity identifiers
 * @param <E> the type of entities to create
 * @author Alexander Yevsyukov
 */
class DefaultEntityFactory<I, E extends AbstractEntity<I, ?>> implements EntityFactory<I, E> {

    private static final long serialVersionUID = 0L;

    private final EntityClass<E> entityClass;

    DefaultEntityFactory(Class<E> entityClass) {
        this.entityClass = new EntityClass<>(entityClass);
    }

    @Override
    public E create(I id) {
        return entityClass.createEntity(id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DefaultEntityFactory other = (DefaultEntityFactory) obj;
        return Objects.equals(this.entityClass, other.entityClass);
    }
}

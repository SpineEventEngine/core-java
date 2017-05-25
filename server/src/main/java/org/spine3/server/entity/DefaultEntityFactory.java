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

package org.spine3.server.entity;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.Objects;

import static org.spine3.server.entity.AbstractEntity.createEntity;
import static org.spine3.server.entity.AbstractEntity.getConstructor;

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

    /** The class of entities to create. */
    private final Class<E> entityClass;

    /** The class of entity IDs. */
    private final Class<I> idClass;

    /**
     * The constructor for creating entity instances.
     *
     * <p>Is {@code null} upon deserialization.
     */
    @Nullable
    private transient Constructor<E> entityConstructor;

    DefaultEntityFactory(Class<E> entityClass, Class<I> idClass) {
        this.entityClass = entityClass;
        this.idClass = idClass;
    }

    private Constructor<E> getEntityConstructor() {
        final Constructor<E> result = getConstructor(entityClass, idClass);
        result.setAccessible(true);
        return result;
    }

    @Override
    public E create(I id) {
        if (entityConstructor == null) {
            entityConstructor = getEntityConstructor();
        }
        return createEntity(this.entityConstructor, id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, idClass);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DefaultEntityFactory other = (DefaultEntityFactory) obj;
        return Objects.equals(this.entityClass, other.entityClass)
                && Objects.equals(this.idClass, other.idClass);
    }
}

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

import java.lang.reflect.Constructor;

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
public class DefaultEntityFactory<I, E extends AbstractEntity<I, ?>> implements EntityFactory<I, E> {

    private final Repository<I, E> repository;

    /** The constructor for creating entity instances. */
    private final Constructor<E> entityConstructor;

    public DefaultEntityFactory(Repository<I, E> repository) {
        this.repository = repository;
        this.entityConstructor = getEntityConstructor();
        this.entityConstructor.setAccessible(true);
    }

    private Constructor<E> getEntityConstructor() {
        final Class<E> entityClass = repository.getEntityClass();
        final Class<I> idClass = repository.getIdClass();
        final Constructor<E> result = getConstructor(entityClass, idClass);
        return result;
    }

    @Override
    public E create(I id) {
        return createEntity(this.entityConstructor, id);
    }
}

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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.Identifiers;
import org.spine3.server.BoundedContext;
import org.spine3.test.ReflectiveBuilder;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for building entities for tests.
 *
 * @param <E> the type of the entity to build
 * @param <I> the type of the entity identifier
 * @param <S> the type of the entity state
 *
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class EntityBuilder<E extends AbstractVersionableEntity<I, S>, I, S extends Message>
       extends ReflectiveBuilder<E> {

    /** The class of the entity IDs. */
    private Class<I> idClass;

    /** The ID of the entity. If not set, a value default to the type will be used. */
    @Nullable
    private I id;

    /** The entity state. If not set, a default instance will be used. */
    @Nullable
    private S state;

    /** The entity version. Or zero if not set. */
    private int version;

    /** The entity timestamp or default {@code Timestamp} if not set. */
    @Nullable
    private Timestamp whenModified;

    /**
     * Creates new instance of the builder.
     */
    public EntityBuilder() {
        super();
        // Have the constructor for finding usages easier.
    }

    @Override
    public EntityBuilder<E, I, S> setResultClass(Class<E> entityClass) {
        super.setResultClass(entityClass);
        this.idClass = getIdClass();
        return this;
    }

    public EntityBuilder<E, I, S> withId(I id) {
        this.id = checkNotNull(id);
        return this;
    }

    public EntityBuilder<E, I, S> withState(S state) {
        this.state = checkNotNull(state);
        return this;
    }

    public EntityBuilder<E, I, S> withVersion(int version) {
        this.version = version;
        return this;
    }

    public EntityBuilder<E, I, S> modifiedOn(Timestamp whenModified) {
        this.whenModified = checkNotNull(whenModified);
        return this;
    }

    /** Returns the class of IDs used by entities. */
    @CheckReturnValue
    protected Class<I> getIdClass() {
        final Class<E> resultClass = getResultClass();
        final Class<I> idClass = Entity.TypeInfo.getIdClass(resultClass);
        return idClass;
    }

    private I createDefaultId() {
        return Identifiers.getDefaultValue(idClass);
    }

    @Override
    public E build() {
        final I id = id();
        final E result = createEntity(id);
        final S state = state(result);
        final Timestamp timestamp = timestamp();

        result.setState(state, version, timestamp);
        return result;
    }

    /**
     * Returns ID if it was previously set or default value if it was not.
     */
    protected I id() {
        return this.id != null
               ? this.id
               : createDefaultId();
    }

    /**
     * Returns state if it was set or the default value if it was not.
     */
    protected S state(E result) {
        return this.state != null
               ? this.state
               : result.getDefaultState();
    }

    /**
     * Returns timestamp if it was set or the default value if it was not.
     */
    protected Timestamp timestamp() {
        return this.whenModified != null
               ? this.whenModified
               : Timestamp.getDefaultInstance();
    }

    @Override
    protected Constructor<E> getConstructor() {
        final Constructor<E> constructor = AbstractEntity.getConstructor(getResultClass(), idClass);
        constructor.setAccessible(true);
        return constructor;
    }

    /**
     * Creates an empty entity instance.
     */
    protected E createEntity(I id) {
        final Constructor<E> constructor = getConstructor();
        final E result = AbstractEntity.createEntity(constructor, id);
        return result;
    }
}

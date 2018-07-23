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

package io.spine.testing.server.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.model.EntityClass;
import io.spine.testing.ReflectiveBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Utility class for building entities for tests.
 *
 * @param <E> the type of the entity to build
 * @param <I> the type of the entity identifier
 * @param <S> the type of the entity state
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class EntityBuilder<E extends AbstractVersionableEntity<I, S>, I, S extends Message>
        extends ReflectiveBuilder<E> {

    /**
     * The class of the entity to build.
     *
     * <p>Is null until {@link #setResultClass(Class)} is called.
     */
    private @Nullable EntityClass<E> entityClass;

    /** The ID of the entity. If not set, a value default to the type will be used. */
    private @Nullable I id;

    /** The entity state. If not set, a default instance will be used. */
    private @Nullable S state;

    /** The entity version. Or zero if not set. */
    private int version;

    /** The entity timestamp or default {@code Timestamp} if not set. */
    private @Nullable Timestamp whenModified;

    /**
     * Creates new instance of the builder.
     */
    public EntityBuilder() {
        super();
        // Have the constructor for finding usages easier.
    }

    @CanIgnoreReturnValue
    @Override
    public EntityBuilder<E, I, S> setResultClass(Class<E> entityClass) {
        super.setResultClass(entityClass);
        this.entityClass = createModelClass(entityClass);
        return this;
    }

    protected EntityClass<E> createModelClass(Class<E> entityClass) {
        return new EntityClass<>(entityClass);
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

    protected EntityClass<E> entityClass() {
        checkState(entityClass != null);
        return entityClass;
    }

    /** Returns the class of IDs used by entities. */
    @SuppressWarnings("unchecked") // The cast is protected by generic parameters of the builder.
    public Class<I> getIdClass() {
        return (Class<I>) entityClass().getIdClass();
    }

    private I createDefaultId() {
        return Identifier.getDefaultValue(getIdClass());
    }

    @Override
    public E build() {
        I id = id();
        E result = createEntity(id);
        S state = state(result);
        Timestamp timestamp = timestamp();

        Version version = Versions.newVersion(this.version, timestamp);
        setState(result, state, version);
        return result;
    }

    protected void setState(E result, S state, Version version) {
        result.updateState(state, version);
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
        Constructor<E> constructor = entityClass().getConstructor();
        constructor.setAccessible(true);
        return constructor;
    }

    /**
     * Creates an empty entity instance.
     */
    protected E createEntity(I id) {
        E result = entityClass().createEntity(id);
        return result;
    }
}

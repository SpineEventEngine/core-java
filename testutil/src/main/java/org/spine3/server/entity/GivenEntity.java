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
import org.spine3.server.reflect.Classes;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for building entities for tests.
 *
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class GivenEntity<I, S extends Message, E extends Entity<I, S>> {

    /** The index of the declaration of the generic type {@code I} in this class. */
    private static final int ID_CLASS_GENERIC_INDEX = 0;

    /** The index of the declaration of the generic parameter type {@code S} in this class. */
    private static final int STATE_CLASS_GENERIC_INDEX = 1;

    /** The class of the entity we create. */
    private Class<E> entityClass;

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

    public GivenEntity<I, S, E> ofClass(Class<E> aggregateClass) {
        this.entityClass = checkNotNull(aggregateClass);
        this.idClass = getIdClass();
        return this;
    }

    public GivenEntity<I, S, E> withId(I id) {
        this.id = checkNotNull(id);
        return this;
    }

    public GivenEntity<I, S, E> withState(S state) {
        this.state = checkNotNull(state);
        return this;
    }

    public GivenEntity<I, S, E> withVersion(int version) {
        this.version = version;
        return this;
    }

    public GivenEntity<I, S, E> modifiedOn(Timestamp whenModified) {
        this.whenModified = checkNotNull(whenModified);
        return this;
    }

    /** Returns the class of IDs used by aggregates. */
    @CheckReturnValue
    protected Class<I> getIdClass() {
        return Classes.getGenericParameterType(getClass(), ID_CLASS_GENERIC_INDEX);
    }

    private I createDefaultId() {
        return Identifiers.getDefaultValue(idClass);
    }

    public E build() {
        final Constructor<E> constructor = Entity.getConstructor(entityClass, idClass);
        constructor.setAccessible(true);

        final I id = this.id != null
                     ? this.id
                     : createDefaultId();

        final E result = Entity.createEntity(constructor, id);

        final S state = this.state != null
                        ? this.state
                        : result.getDefaultState();

        final Timestamp timestamp = this.whenModified != null
                                    ? this.whenModified
                                    : Timestamp.getDefaultInstance();

        result.setState(state, version, timestamp);
        return result;
    }
}

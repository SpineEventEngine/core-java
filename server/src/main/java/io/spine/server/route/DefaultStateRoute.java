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

package io.spine.server.route;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.protobuf.Messages;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.findField;
import static io.spine.server.route.EventRoute.withId;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Obtains the route as a value of the first field matching the type of the identifiers.
 *
 * <p>Descriptors of discovered fields are cached.
 *
 * <p>If a passed message does not have a field of the required ID type
 * {@code IllegalStateException} will be thrown.
 *
 * @param <I>
 *         the type of the identifiers
 */
final class DefaultStateRoute<I> implements StateUpdateRoute<I, Message> {

    private static final long serialVersionUID = 0L;

    private final Class<I> idClass;

    /**
     * Descriptors of fields matching the ID class by message type.
     */
    private final ConcurrentHashMap<Class<? extends Message>, FieldDescriptor> fields =
            new ConcurrentHashMap<>();

    private DefaultStateRoute(Class<I> idClass) {
        this.idClass = idClass;
    }

    /**
     * Creates a new instance.
     *
     * @param idClass
     *         the class of identifiers used for the routing
     */
    public static <I> DefaultStateRoute<I> newInstance(Class<I> idClass) {
        checkNotNull(idClass);
        return new DefaultStateRoute<>(idClass);
    }

    boolean supports(Class<? extends Message> stateType) {
        Descriptor type = Messages.defaultInstance(stateType)
                                  .getDescriptorForType();
        Optional<FieldDescriptor> idField = findField(idClass, type);
        return idField.isPresent();
    }

    /**
     * Obtains the ID from the first field of the passed message that matches the type
     * of identifiers used by this route.
     *
     * <p>If the such a field is discovered, its descriptor is remembered and associated
     * with the class of the state so that subsequent calls are faster.
     *
     * <p>If a field matching the ID type is not found, the method
     * throws {@code IllegalStateException}.
     *
     * @param state
     *         the entity state message
     * @param ignored
     *         the context of the update event, not used
     * @return a one-element set with the ID
     * @throws IllegalStateException
     *          if the passed state instance does not have a field of the required ID type
     */
    @Override
    public Set<I> apply(Message state, EventContext ignored) {
        checkNotNull(state);
        checkNotNull(ignored);
        Class<? extends Message> messageClass = state.getClass();
        FieldDescriptor field = fields.get(messageClass);
        if (field != null) {
            return fieldToSet(field, state);
        }

        FieldDescriptor fd = findField(idClass, state.getDescriptorForType())
                .orElseThrow(() -> newIllegalStateException(
                        "Unable to find a field matching the type `%s`" +
                                " in the message of the type `%s`.",
                        idClass, messageClass.getCanonicalName()));
        fields.put(messageClass, fd);
        Set<I> result = fieldToSet(fd, state);
        return result;
    }

    private Set<I> fieldToSet(FieldDescriptor field, Message message) {
        Object fieldValue = message.getField(field);
        I id = idClass.cast(fieldValue);
        return withId(id);
    }
}

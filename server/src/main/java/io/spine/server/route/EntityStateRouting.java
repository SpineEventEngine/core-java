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

package io.spine.server.route;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.model.EntityStateClass;
import io.spine.system.server.EntityStateChanged;

import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;

public class EntityStateRouting<I>
        extends MessageRouting<Message, EventContext, EntityStateClass, Set<I>> {

    private static final long serialVersionUID = 0L;

    private EntityStateRouting() {
        super(((message, context) -> of()));
    }

    public static <I> EntityStateRouting<I> newInstance() {
        return new EntityStateRouting<>();
    }

    @Override
    EntityStateClass toMessageClass(Class<? extends Message> classOfMessages) {
        return EntityStateClass.from(classOfMessages);
    }

    @Override
    EntityStateClass toMessageClass(Message outerOrMessage) {
        return EntityStateClass.of(outerOrMessage);
    }

    @CanIgnoreReturnValue
    public <E extends Message> EntityStateRouting<I> route(Class<E> stateClass,
                                                                EntityStateRoute<I, E> via)
            throws IllegalStateException {
        @SuppressWarnings("unchecked") // Logically valid.
        Route<Message, EventContext, Set<I>> route = (Route<Message, EventContext, Set<I>>) via;
        doRoute(stateClass, route);
        return this;
    }

    EventRoute<I, EntityStateChanged> eventRoute() {
        return (message, context) -> {
            Message state = AnyPacker.unpack(message.getNewState());
            return apply(state, context);
        };
    }
}

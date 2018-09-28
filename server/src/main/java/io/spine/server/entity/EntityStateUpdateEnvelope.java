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

import com.google.common.base.Objects;
import com.google.protobuf.Message;
import io.spine.core.AbstractMessageEnvelope;
import io.spine.core.EventContext;
import io.spine.server.entity.model.EntityStateClass;
import io.spine.system.server.DispatchedMessageId;
import io.spine.system.server.EntityStateChanged;
import io.spine.type.MessageClass;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Dmytro Dashenkov
 */
public class EntityStateUpdateEnvelope
        extends AbstractMessageEnvelope<DispatchedMessageId, EntityStateChanged, EventContext> {

    private final DispatchedMessageId id;
    private final EventContext context;

    private final Message state;

    private EntityStateUpdateEnvelope(DispatchedMessageId id,
                                      EntityStateChanged systemEvent,
                                      EventContext context) {
        super(systemEvent);
        this.id = id;
        this.context = context;
        this.state = unpack(systemEvent.getNewState());

    }

    public static EntityStateUpdateEnvelope of(EntityStateChanged systemEvent,
                                               EventContext context) {
        checkNotNull(systemEvent);
        checkNotNull(context);
        checkArgument(systemEvent.getMessageIdCount() > 0, "Message IDs must not be empty.");

        DispatchedMessageId firstMessageId = systemEvent.getMessageId(0);
        return new EntityStateUpdateEnvelope(firstMessageId, systemEvent, context);
    }

    @Override
    public DispatchedMessageId getId() {
        return id;
    }

    @Override
    public Message getMessage() {
        return state;
    }

    @Override
    public MessageClass getMessageClass() {
        return EntityStateClass.of(state);
    }

    @Override
    public EventContext getMessageContext() {
        return context;
    }

    @Override
    public void setOriginFields(EventContext.Builder builder) {
        throw newIllegalStateException("An entity update cannot originate messages.");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        EntityStateUpdateEnvelope envelope = (EntityStateUpdateEnvelope) o;
        return Objects.equal(id, envelope.id) &&
                Objects.equal(context, envelope.context);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), id, context);
    }
}

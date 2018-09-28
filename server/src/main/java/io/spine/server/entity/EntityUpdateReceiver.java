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

import com.google.protobuf.Message;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.server.entity.model.EntityStateClass;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.system.server.EntityStateChanged;

import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.EntityStateUpdateEnvelope.of;

/**
 * @author Dmytro Dashenkov
 */
public class EntityUpdateReceiver extends AbstractEventSubscriber {

    private final EntityStateDispatcher dispatcher;

    public EntityUpdateReceiver(EntityStateDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Subscribe(external = true)
    public void on(EntityStateChanged event, EventContext context) {
        Message entityState = unpack(event.getNewState());
        EntityStateClass type = EntityStateClass.of(entityState);
        boolean dispatcherReceivesType = dispatcher.getEntitySubscriptionClasses().contains(type);
        if (dispatcherReceivesType) {
            EntityStateUpdateEnvelope envelope = of(event, context);
            dispatch(envelope);
        }
    }

    private void dispatch(EntityStateUpdateEnvelope envelope) {
        try {
            dispatcher.dispatchEntityUpdate(envelope);
        } catch (RuntimeException e) {
            dispatcher.onError(envelope, e);
        }
    }
}

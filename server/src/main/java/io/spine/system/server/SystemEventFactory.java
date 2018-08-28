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

package io.spine.system.server;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.core.ActorContext;
import io.spine.core.EmptyClass;
import io.spine.core.EventContext;
import io.spine.core.MessageEnvelope;
import io.spine.server.event.EventFactory;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.system.server.SystemCommandFactory.requestFactory;

/**
 * Creates events that will be imported into system aggregates.
 *
 * @author Alexander Yevsyukov
 */
final class SystemEventFactory extends EventFactory {

    SystemEventFactory(Message aggregateId, boolean multitenant) {
        super(ImportOrigin.newInstance(multitenant), Identifier.pack(aggregateId));
    }

    /**
     * A rudimentary implementation of {@link MessageEnvelope} with the sole purpose
     * of setting {@link EventContext.Builder#setImportContext(ActorContext) import context}
     * in events created for import into system aggregates.
     */
    private static final class ImportOrigin
            implements MessageEnvelope<Empty, Empty, Empty> {

        private final ActorContext actorContext;

        private ImportOrigin(ActorContext context) {
            this.actorContext = checkNotNull(context);
        }

        static ImportOrigin newInstance(boolean multitenant) {
            ActorContext actorContext = requestFactory(multitenant).newActorContext();
            return new ImportOrigin(actorContext);
        }

        @Override
        @SuppressWarnings("CheckReturnValue") // calling builder
        public void setOriginFields(EventContext.Builder builder) {
            builder.setImportContext(actorContext);
        }

        @Override
        public Empty getId() {
            return Empty.getDefaultInstance();
        }

        @Override
        public Empty getOuterObject() {
            return Empty.getDefaultInstance();
        }

        @Override
        public Message getMessage() {
            return Empty.getDefaultInstance();
        }

        @Override
        public MessageClass getMessageClass() {
            return EmptyClass.instance();
        }

        @Override
        public Empty getMessageContext() {
            return Empty.getDefaultInstance();
        }
    }
}

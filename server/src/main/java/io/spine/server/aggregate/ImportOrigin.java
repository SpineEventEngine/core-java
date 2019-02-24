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

package io.spine.server.aggregate;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.ActorContext;
import io.spine.core.EventContext;
import io.spine.server.type.EmptyClass;
import io.spine.server.type.MessageEnvelope;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A rudimentary implementation of {@link MessageEnvelope} with the sole purpose
 * of setting {@link io.spine.core.EventContext.Builder#setImportContext(ActorContext)
 * import context} in events created for import into system aggregates.
 */
public final class ImportOrigin implements MessageEnvelope<Empty, Empty, Empty> {

    private final ActorContext actorContext;

    /**
     * Creates a new instance with the passed actor context.
     */
    public static ImportOrigin newInstance(ActorContext context) {
        checkNotNull(context);
        return new ImportOrigin(context);
    }

    private ImportOrigin(ActorContext context) {
        this.actorContext = checkNotNull(context);
    }

    @Override
    @SuppressWarnings("CheckReturnValue") // calling builder
    public void setOriginFields(EventContext.Builder builder) {
        builder.setImportContext(actorContext);
    }

    @Override
    public Empty id() {
        return Empty.getDefaultInstance();
    }

    @Override
    public Empty outerObject() {
        return Empty.getDefaultInstance();
    }

    @Override
    public Message message() {
        return Empty.getDefaultInstance();
    }

    @Override
    public MessageClass messageClass() {
        return EmptyClass.instance();
    }

    @Override
    public Empty messageContext() {
        return Empty.getDefaultInstance();
    }
}

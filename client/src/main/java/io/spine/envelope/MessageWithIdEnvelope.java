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

package io.spine.envelope;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.MessageAcked;
import io.spine.base.Status;
import io.spine.protobuf.AnyPacker;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Responses.statusOk;

/**
 * An abstract {@code MessageEnvelope} or the messages which declare an ID.
 *
 * @author Dmytro Dashenkov
 */
public abstract class MessageWithIdEnvelope<I extends Message, T>
        extends AbstractMessageEnvelope<T> {

    MessageWithIdEnvelope(T object) {
        super(object);
    }

    /**
     * Obtains the identifier of the object.
     */
    public abstract I getId();

    public final MessageAcked acknowledge() {
        return acknowledge(statusOk());
    }

    public final MessageAcked acknowledge(Status status) {
        checkNotNull(status);
        final I id = getId();
        final Any packedId = AnyPacker.pack(id);
        final MessageAcked result = MessageAcked.newBuilder()
                                                .setMessageId(packedId)
                                                .setStatus(status)
                                                .build();
        return result;
    }
}

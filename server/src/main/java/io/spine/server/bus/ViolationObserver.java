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

package io.spine.server.bus;

import io.grpc.stub.StreamObserver;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.MessageId;
import io.spine.core.MessageWithContext;
import io.spine.core.Status;
import io.spine.grpc.DelegatingObserver;
import io.spine.system.server.ConstraintViolated;
import io.spine.system.server.SystemWriteSide;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationError;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Status.StatusCase.ERROR;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.validate.Validate.isNotDefault;

final class ViolationObserver extends DelegatingObserver<Ack> {

    private final SystemWriteSide system;
    private final Map<MessageId, MessageWithContext<?, ?, ?>> messages;

    ViolationObserver(StreamObserver<? super Ack> delegate,
                      SystemWriteSide system,
                      Map<MessageId, MessageWithContext<?, ?, ?>> messages) {
        super(delegate);
        this.system = checkNotNull(system);
        this.messages = checkNotNull(messages);
    }

    @Override
    public void onNext(Ack ack) {
        Status status = ack.getStatus();
        if (status.getStatusCase() == ERROR) {
            MessageId messageId = (MessageId) unpack(ack.getMessageId());
            postIfViolations(messageId, status.getError());
        }
        super.onNext(ack);
    }

    private void postIfViolations(MessageId messageId, Error error) {
        ValidationError validationError = error.getValidationError();
        if (isNotDefault(validationError)) {
            List<ConstraintViolation> violations = validationError.getConstraintViolationList();
            MessageWithContext<?, ?, ?> message = messages.get(messageId);
            ConstraintViolated event = ConstraintViolated
                    .newBuilder()
                    .addAllViolation(violations)
                    .setRootMessage(message.rootMessage())
                    .setLastMessage(message.qualifier())
                    .vBuild();
            system.notifySystem(event);
        }
    }
}

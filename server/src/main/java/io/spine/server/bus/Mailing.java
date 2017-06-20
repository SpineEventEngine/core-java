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

package io.spine.server.bus;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.IsSent;
import io.spine.base.Responses;
import io.spine.base.Status;
import io.spine.envelope.MessageWithIdEnvelope;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.validate.Validate.isNotDefault;

/**
 * @author Dmytro Dashenkov
 */
@Internal
public final class Mailing {

    private Mailing() {
        // Prevent utility class instantiation.
    }

    public static IsSent checkIn(MessageWithIdEnvelope<?, ?> envelope) {
        return checkIn(envelope, Responses.statusOk());
    }

    public static IsSent checkIn(MessageWithIdEnvelope<?, ?> envelope, Status status) {
        checkNotNull(envelope);
        checkNotNull(status);
        checkArgument(isNotDefault(status));

        final Message id = envelope.getId();
        final Any packedId = pack(id);
        final IsSent result = IsSent.newBuilder()
                                    .setMessageId(packedId)
                                    .setStatus(status)
                                    .build();
        return result;
    }
}

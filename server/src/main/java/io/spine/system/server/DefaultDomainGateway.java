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

import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.delivery.NonDeliveryNotice;
import io.spine.server.delivery.Parcel;
import io.spine.server.delivery.ParcelReceiver;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventDispatcher;
import io.spine.type.TypeUrl;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static io.spine.json.Json.toJson;

/**
 * @author Dmytro Dashenkov
 */
public class DefaultDomainGateway implements DomainGateway {

    private final BoundedContext domain;

    public DefaultDomainGateway(BoundedContext domain) {
        this.domain = domain;
    }

    @Override
    public void deliver(Parcel parcel) {
        ParcelReceiver receiver = parcel.getReceiver();
        TypeUrl type = TypeUrl.parse(receiver.getTypeUrl());
        Class<Message> receiverType = type.getMessageClass();
        Optional<Repository> repository = domain.findRepository(receiverType);
        checkArgument(repository.isPresent(), "cannot find repository for type `%s`.", type);
        switch (parcel.getPayloadCase()) {
            case COMMAND: {
                CommandDispatcher<?> dispatcher = (CommandDispatcher<?>) repository.get();
                CommandEnvelope envelope = CommandEnvelope.of(parcel.getCommand());
                dispatcher.dispatch(envelope);
                break;
            }
            case EVENT: {
                EventDispatcher<?> dispatcher = (EventDispatcher<?>) repository.get();
                EventEnvelope envelope = EventEnvelope.of(parcel.getEvent());
                dispatcher.dispatch(envelope);
                break;
            }
            case PAYLOAD_NOT_SET:
            default:
                throw new IllegalArgumentException(toJson(parcel));
        }
    }

    @Override
    public void deliver(NonDeliveryNotice notice) {

    }
}

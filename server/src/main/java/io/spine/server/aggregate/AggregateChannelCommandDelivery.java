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
package io.spine.server.aggregate;

import io.spine.annotation.SPI;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessages;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.TransportFactory;

import java.util.Set;

/**
 * An abstract base for {@code Aggregate} {@linkplain AggregateCommandDelivery command
 * delivery strategies}, which use {@linkplain io.spine.server.transport.MessageChannel channels}
 * to bring command messages to the aggregate instances.
 *
 * <p>The implementations of this class provide their strategy on how to define the channel IDs
 * per message per target entity ID.
 *
 * @author Alex Tymchenko
 */
@SPI
public abstract class AggregateChannelCommandDelivery<I, A extends Aggregate<I, ?, ?>>
        extends AggregateCommandDelivery<I, A> {

    private final TransportFactory transportFactory;
    private final BoundedContextName boundedContextName;

    protected AggregateChannelCommandDelivery(AggregateRepository<I, A> repository,
                                              TransportFactory transportFactory,
                                              BoundedContextName boundedContextName) {
        super(repository);
        this.transportFactory = transportFactory;
        this.boundedContextName = boundedContextName;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always postpone the message from the immediate delivery and forward them to one or more
     * channels instead.
     *
     * @param id       the ID of the entity the envelope is going to be dispatched.
     * @param envelope the envelope to be dispatched — now or later
     * @return
     */
    @Override
    public boolean shouldPostpone(I id, CommandEnvelope envelope) {
        sendToChannels(id, envelope);
        return true;
    }

    private void sendToChannels(I id, CommandEnvelope envelope) {
        final Set<ChannelId> channelIds = channelsFor(id, envelope);
        for (ChannelId channelId : channelIds) {
            final Publisher publisher = ensurePublisher(channelId);
            final ExternalMessage externalMessage = ExternalMessages.of(envelope.getCommand(),
                                                                        boundedContextName);
            final Ack ack = publisher.publish(externalMessage.getId(), externalMessage);
            if (!ack.getStatus().hasOk()) {
                //TODO:2018-02-15:alex.tymchenko: figure out what to do in this case.
            }
        }
    }

    protected abstract Set<ChannelId> channelsFor(I id, CommandEnvelope commandEnvelope);

    /**
     * Ensures that the publisher channel is reachable by the passed identifier.
     *
     * <p>Relies onto the {@linkplain io.spine.server.transport.TransportFactory transport factory}.
     *
     * @param channelId an ID of a channel to establish
     * @return an instance of the message channel for the given {@code ChannelId}
     */
    private Publisher ensurePublisher(ChannelId channelId) {
        final Publisher result = transportFactory.createPublisher(channelId);
        return result;
    }
}

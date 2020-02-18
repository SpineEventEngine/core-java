/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import io.spine.core.SignalId;
import io.spine.core.TenantId;
import io.spine.server.bus.MulticastDispatchListener;
import io.spine.server.tenant.TenantAwareRunner;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.Multimaps.synchronizedListMultimap;
import static java.util.Collections.synchronizedSet;

/**
 * Listens to the signals dispatched via the {@code MulticastBus}es and notifies the shard
 * observers of a new {@code InboxMessage} only when it has been dispatched to all of
 * its multicast targets.
 */
final class DeliveryDispatchListener implements MulticastDispatchListener {

    private final Multimap<SignalId, InboxMessage> pending =
            synchronizedListMultimap(MultimapBuilder.hashKeys()
                                                    .arrayListValues()
                                                    .build());

    private final Set<SignalId> currentlyDispatching = synchronizedSet(new HashSet<>());

    private final Consumer<InboxMessage> onNewMessage;

    DeliveryDispatchListener(Consumer<InboxMessage> message) {
        onNewMessage = message;
    }

    @Override
    public void onStarted(SignalId signal) {
        currentlyDispatching.add(signal);
    }

    @Override
    public void onCompleted(SignalId signal) {
        boolean removed = currentlyDispatching.remove(signal);
        if (removed) {
            Collection<InboxMessage> messages = pending.removeAll(signal);
            for (InboxMessage message : messages) {
                propagateMessage(message);
            }
        }
    }

    /**
     * Notifies the subscribed parties of a new message if it not currently dispatching.
     *
     * @param message
     *         the message to notify of
     */
    void notifyOf(InboxMessage message) {
        SignalId id = message.hasEvent()
                      ? message.getEvent()
                               .getId()
                      : message.getCommand()
                               .getId();
        if (currentlyDispatching.contains(id)) {
            pending.put(id, message);
        } else {
            propagateMessage(message);
        }
    }

    private void propagateMessage(InboxMessage message) {
        TenantId tenant =
                message.hasEvent() ? message.getEvent()
                                            .tenant()
                                   : message.getCommand()
                                            .tenant();
        TenantAwareRunner
                .with(tenant)
                .run(() -> onNewMessage.accept(message));
    }
}

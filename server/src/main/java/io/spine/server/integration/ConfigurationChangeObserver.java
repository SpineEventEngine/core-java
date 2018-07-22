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
package io.spine.server.integration;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.core.BoundedContextName;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An observer, which reacts to the configuration update messages sent by
 * external entities (such as {@code IntegrationBus}es of other bounded contexts).
 *
 * @author Alex Tymchenko
 */
final class ConfigurationChangeObserver extends AbstractChannelObserver implements AutoCloseable {

    private final BoundedContextName boundedContextName;
    private final Function<Class<? extends Message>, BusAdapter<?, ?>> adapterByClass;

    /**
     * Current set of message type URLs, requested by other parties via sending the
     * {@linkplain RequestForExternalMessages configuration messages}, mapped to IDs of their origin
     * bounded contexts.
     */
    private final Multimap<ExternalMessageType, BoundedContextName> requestedTypes =
            HashMultimap.create();

    ConfigurationChangeObserver(BoundedContextName boundedContextName,
                                Function<Class<? extends Message>, BusAdapter<?, ?>> adapterByCls) {
        super(boundedContextName, RequestForExternalMessages.class);
        this.boundedContextName = boundedContextName;
        this.adapterByClass = adapterByCls;
    }

    @Override
    public void handle(ExternalMessage value) {
        RequestForExternalMessages request = AnyPacker.unpack(value.getOriginalMessage());

        BoundedContextName origin = value.getBoundedContextName();
        addNewSubscriptions(request.getRequestedMessageTypesList(), origin);
        clearStaleSubscriptions(request.getRequestedMessageTypesList(), origin);
    }

    private void addNewSubscriptions(Iterable<ExternalMessageType> types,
                                     BoundedContextName origin) {
        for (ExternalMessageType newType : types) {
            Collection<BoundedContextName> contextsWithSameRequest = requestedTypes.get(newType);
            if (contextsWithSameRequest.isEmpty()) {

                // This item has not been requested by anyone yet.
                // Let's create a subscription.
                registerInAdapter(newType);
            }

            requestedTypes.put(newType, origin);
        }
    }

    private void registerInAdapter(ExternalMessageType newType) {
        Class<Message> wrapperCls = asClassOfMsg(newType.getWrapperTypeUrl());
        Class<Message> messageCls = asClassOfMsg(newType.getMessageTypeUrl());
        BusAdapter<?, ?> adapter = getAdapter(wrapperCls);
        adapter.register(messageCls);
    }

    private BusAdapter<?, ?> getAdapter(Class<Message> javaClass) {
        BusAdapter<?, ?> adapter = adapterByClass.apply(javaClass);
        return checkNotNull(adapter);
    }

    private void clearStaleSubscriptions(Collection<ExternalMessageType> types,
                                         BoundedContextName origin) {

        Set<ExternalMessageType> toRemove = findStale(types, origin);

        for (ExternalMessageType itemForRemoval : toRemove) {
            boolean wereNonEmpty = !requestedTypes.get(itemForRemoval)
                                                  .isEmpty();
            requestedTypes.remove(itemForRemoval, origin);
            boolean emptyNow = requestedTypes.get(itemForRemoval)
                                             .isEmpty();

            if (wereNonEmpty && emptyNow) {
                unregisterInAdapter(itemForRemoval);
            }
        }
    }

    private void unregisterInAdapter(ExternalMessageType itemForRemoval) {
        // It's now the time to remove the local bus subscription.
        Class<Message> wrapperCls = asClassOfMsg(itemForRemoval.getWrapperTypeUrl());
        Class<Message> messageCls = asClassOfMsg(itemForRemoval.getMessageTypeUrl());
        BusAdapter<?, ?> adapter = getAdapter(wrapperCls);
        adapter.unregister(messageCls);
    }

    private Set<ExternalMessageType> findStale(Collection<ExternalMessageType> types,
                                               BoundedContextName origin) {
        ImmutableSet.Builder<ExternalMessageType> result = ImmutableSet.builder();

        for (ExternalMessageType previouslyRequestedType : requestedTypes.keySet()) {
            Collection<BoundedContextName> contextsThatRequested =
                    requestedTypes.get(previouslyRequestedType);

            if (contextsThatRequested.contains(origin) &&
                    !types.contains(previouslyRequestedType)) {

                // The `previouslyRequestedType` item is no longer requested
                // by the bounded context with `origin` name.

                result.add(previouslyRequestedType);
            }
        }
        return result.build();
    }

    @Override
    public String toString() {
        return "Integration bus observer of `RequestedMessageTypes`. " +
                "Bounded Context name = " + boundedContextName.getValue();
    }

    private static Class<Message> asClassOfMsg(String classStr) {
        TypeUrl typeUrl = TypeUrl.parse(classStr);
        return typeUrl.getMessageClass();
    }

    /**
     * Removes all the current subscriptions from the local buses.
     */
    @Override
    public void close() throws Exception {
        for (ExternalMessageType currentlyRequestedMessage : requestedTypes.keySet()) {
            unregisterInAdapter(currentlyRequestedMessage);
        }
        requestedTypes.clear();
    }
}

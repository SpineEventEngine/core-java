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
package io.spine.server.integration;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Message;
import io.spine.core.BoundedContextId;
import io.spine.protobuf.AnyPacker;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.newHashSet;

/**
 * An observer, which reacts to the configuration update messages sent by
 * external entities (such as {@code IntegrationBus}es of other bounded contexts).
 *
 * @author Alex Tymchenko
 */
class ConfigurationChangeObserver extends ChannelObserver {

    private final BoundedContextId boundedContextId;
    private final Function<Class<? extends Message>, BusAdapter<?, ?>> adapterByClass;

    /**
     * Current set of message type URLs, requested by other parties via sending the
     * {@linkplain RequestedMessageTypes configuration messages}, mapped to IDs of their origin
     * bounded contexts.
     */
    private final Multimap<String, BoundedContextId> requestedTypes = HashMultimap.create();

    ConfigurationChangeObserver(BoundedContextId boundedContextId,
                                Function<Class<? extends Message>,
                                        BusAdapter<?, ?>> adapterByClass) {
        super(boundedContextId, RequestedMessageTypes.class);
        this.boundedContextId = boundedContextId;
        this.adapterByClass = adapterByClass;
    }

    @Override
    public void handle(IntegrationMessage value) {
        final RequestedMessageTypes message = AnyPacker.unpack(value.getOriginalMessage());
        final Set<String> newTypeUrls = newHashSet(message.getTypeUrlsList());

        final BoundedContextId originBoundedContextId = value.getBoundedContextId();
        addNewSubscriptions(newTypeUrls, originBoundedContextId);
        clearStaleSubscriptions(newTypeUrls, originBoundedContextId);
    }

    private void addNewSubscriptions(Set<String> newTypeUrls,
                                     BoundedContextId originBoundedContextId) {
        for (String newRequestedUrl : newTypeUrls) {
            final Collection<BoundedContextId> contextsWithSameRequest =
                    requestedTypes.get(newRequestedUrl);
            if (contextsWithSameRequest.isEmpty()) {

                // This item has is not requested by anyone at the moment.
                // Let's create a subscription.

                final Class<Message> javaClass = asClassOfMsg(newRequestedUrl);
                final BusAdapter<?, ?> adapter = getAdapter(javaClass);
                adapter.register(javaClass);

            }

            requestedTypes.put(newRequestedUrl, originBoundedContextId);
        }
    }

    private BusAdapter<?, ?> getAdapter(Class<Message> javaClass) {
        final BusAdapter<?, ?> adapter = adapterByClass.apply(javaClass);
        return checkNotNull(adapter);
    }

    private void clearStaleSubscriptions(Set<String> newTypeUrls,
                                         BoundedContextId originBoundedContextId) {

        final Set<String> toRemove = findToRemove(newTypeUrls, originBoundedContextId);

        for (String itemForRemoval : toRemove) {
            final boolean wereNonEmpty = !requestedTypes.get(itemForRemoval)
                                                        .isEmpty();
            requestedTypes.remove(itemForRemoval, originBoundedContextId);
            final boolean emptyNow = requestedTypes.get(itemForRemoval)
                                                   .isEmpty();

            if (wereNonEmpty && emptyNow) {
                // It's now the time to remove the local bus subscription.
                final Class<Message> javaClass = asClassOfMsg(itemForRemoval);
                final BusAdapter<?, ?> adapter = getAdapter(javaClass);
                adapter.unregister(javaClass);
            }
        }
    }

    private Set<String> findToRemove(Set<String> newTypeUrls,
                                     BoundedContextId originBoundedContextId) {
        final Set<String> result = newHashSet();

        for (String previouslyRequestedType : requestedTypes.keySet()) {
            final Collection<BoundedContextId> contextsThatRequested =
                    requestedTypes.get(previouslyRequestedType);
            if (contextsThatRequested.contains(originBoundedContextId) &&
                    !newTypeUrls.contains(previouslyRequestedType)) {

                // The `previouslyRequestedType` item is no longer requested
                // by the bounded context with `originBoundedContextId` ID.

                result.add(previouslyRequestedType);
            }
        }
        return copyOf(result);
    }

    @Override
    public String toString() {
        return "Integration bus observer of `RequestedMessageTypes`; " +
                "Bounded Context ID = " + boundedContextId.getValue();
    }

    private static Class<Message> asClassOfMsg(String classStr) {
        final TypeUrl typeUrl = TypeUrl.parse(classStr);
        return typeUrl.getJavaClass();
    }
}

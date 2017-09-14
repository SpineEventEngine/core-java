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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import io.spine.core.BoundedContextId;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.server.rejection.RejectionSubscriber;

import java.util.Objects;
import java.util.Set;

/**
 * A subscriber to local {@code RejectionBus}, which publishes each matching message to
 * a remote channel.
 *
 * <p>The messages to subscribe are those that are required by external application components
 * at this moment; their set is determined by the {@linkplain RequestForExternalMessages
 * configuration messages}, received by this instance of {@code IntegrationBus}.
 */
final class LocalRejectionSubscriber extends RejectionSubscriber {

    private final BoundedContextId boundedContextId;
    private final TransportFactory.PublisherHub publisherHub;
    private final Set<RejectionClass> rejectionClasses;

    LocalRejectionSubscriber(BoundedContextId boundedContextId,
                             TransportFactory.PublisherHub publisherHub,
                             RejectionClass rejectionClass) {
        super();
        this.boundedContextId = boundedContextId;
        this.publisherHub = publisherHub;
        this.rejectionClasses = ImmutableSet.of(rejectionClass);
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")    // Returning an immutable impl.
    @Override
    public Set<RejectionClass> getMessageClasses() {
        return rejectionClasses;
    }

    @Override
    public Set<String> dispatch(RejectionEnvelope envelope) {
        final Rejection rejection = envelope.getOuterObject();
        final ExternalMessage message = ExternalMessages.of(rejection, boundedContextId);
        final ExternalMessageClass messageClass =
                ExternalMessageClass.of(envelope.getMessageClass());
        final TransportFactory.Publisher channel = publisherHub.get(messageClass);
        channel.publish(AnyPacker.pack(envelope.getId()), message);

        return ImmutableSet.of(channel.toString());
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("boundedContextId", boundedContextId)
                          .add("rejectionClasses", rejectionClasses)
                          .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalRejectionSubscriber that = (LocalRejectionSubscriber) o;
        return Objects.equals(boundedContextId, that.boundedContextId) &&
                Objects.equals(rejectionClasses, that.rejectionClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundedContextId, rejectionClasses);
    }
}

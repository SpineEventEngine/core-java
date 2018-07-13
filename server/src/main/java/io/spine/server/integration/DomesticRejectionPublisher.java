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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import io.spine.core.BoundedContextName;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.PublisherHub;

import java.util.Objects;
import java.util.Set;

/**
 * A subscriber to local {@code RejectionBus}, which publishes each matching domestic rejection to
 * a remote channel.
 *
 * <p>The rejections to subscribe are those that are required by external application components
 * at this moment; their set is determined by the {@linkplain RequestForExternalMessages
 * configuration messages}, received by this instance of {@code IntegrationBus}.
 *
 * @author Alex Tymchenko
 */
final class DomesticRejectionPublisher extends RejectionSubscriber {

    private final BoundedContextName boundedContextName;
    private final PublisherHub publisherHub;
    private final Set<RejectionClass> rejectionClasses;

    DomesticRejectionPublisher(BoundedContextName boundedContextName,
                               PublisherHub publisherHub,
                               RejectionClass rejectionClass) {
        super();
        this.boundedContextName = boundedContextName;
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
        Rejection rejection = envelope.getOuterObject();
        ExternalMessage message = ExternalMessages.of(rejection, boundedContextName);
        ExternalMessageClass messageClass =
                ExternalMessageClass.of(envelope.getMessageClass());
        ChannelId channelId = IntegrationChannels.toId(messageClass);
        Publisher channel = publisherHub.get(channelId);
        channel.publish(AnyPacker.pack(envelope.getId()), message);

        return ImmutableSet.of(channel.toString());
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("boundedContextName", boundedContextName)
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
        DomesticRejectionPublisher that = (DomesticRejectionPublisher) o;
        return Objects.equals(boundedContextName, that.boundedContextName) &&
                Objects.equals(rejectionClasses, that.rejectionClasses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundedContextName, rejectionClasses);
    }
}

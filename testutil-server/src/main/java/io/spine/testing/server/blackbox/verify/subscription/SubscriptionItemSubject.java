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

package io.spine.testing.server.blackbox.verify.subscription;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Topic;

import java.util.List;
import java.util.function.Consumer;

import static com.google.common.truth.Truth.assertAbout;
import static java.util.stream.Collectors.toList;

/**
 * A set of checks for a single item received via {@link SubscriptionUpdate}.
 *
 * <p>Should not be created in the client code directly. Instead, is provided to the callers of
 * {@link io.spine.testing.server.blackbox.BlackBoxBoundedContext#assertSubscriptionUpdates(
 * Topic, Consumer)} method.
 */
public final class SubscriptionItemSubject
        extends ProtoSubject<SubscriptionItemSubject, Message> {

    private SubscriptionItemSubject(FailureMetadata failureMetadata, Message message) {
        super(failureMetadata, message);
    }

    private static SubscriptionItemSubject assertMessage(Message message) {
        return assertAbout(subscriptionItem()).that(message);
    }

    @Internal
    public static Iterable<SubscriptionItemSubject> collectAll(SubscriptionUpdate update) {
        switch (update.getUpdateCase()) {
            case ENTITY_UPDATES:
                return collectEntitySubjects(update);
            case EVENT_UPDATES:
                return collectEventSubjects(update);
            case UPDATE_NOT_SET:
            default:
                return ImmutableList.of();
        }
    }

    private static Iterable<SubscriptionItemSubject>
    collectEntitySubjects(SubscriptionUpdate update) {
        return toSubjects(update.states());
    }

    private static Iterable<SubscriptionItemSubject>
    collectEventSubjects(SubscriptionUpdate update) {
        return toSubjects(update.eventMessages());
    }

    private static Iterable<SubscriptionItemSubject> toSubjects(List<? extends Message> messages) {
        List<SubscriptionItemSubject> result =
                messages.stream()
                        .map(SubscriptionItemSubject::assertMessage)
                        .collect(toList());
        return result;
    }

    static Subject.Factory<SubscriptionItemSubject, Message> subscriptionItem() {
        return SubscriptionItemSubject::new;
    }
}

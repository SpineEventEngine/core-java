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

package io.spine.testing.server.blackbox;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Message;
import io.spine.client.SubscriptionUpdate;

import java.util.List;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Converts the given {@link SubscriptionUpdate} to the proto subjects on a per-item basis.
 */
final class ToProtoSubjects
        implements Function<SubscriptionUpdate, Iterable<ProtoSubject>> {

    @Override
    public Iterable<ProtoSubject> apply(SubscriptionUpdate update) {
        checkNotNull(update);
        return collectAll(update);
    }

    private static Iterable<ProtoSubject> collectAll(SubscriptionUpdate update) {
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

    private static Iterable<ProtoSubject>
    collectEntitySubjects(SubscriptionUpdate update) {
        return toSubjects(update.states());
    }

    private static Iterable<ProtoSubject>
    collectEventSubjects(SubscriptionUpdate update) {
        return toSubjects(update.eventMessages());
    }

    private static Iterable<ProtoSubject>
    toSubjects(List<? extends Message> messages) {
        List<ProtoSubject> result =
                messages.stream()
                        .map(ProtoTruth::assertThat)
                        .collect(toList());
        return result;
    }
}

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

package io.spine.server.stand;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Subscriptions;
import io.spine.client.Target;
import io.spine.client.Targets;
import io.spine.protobuf.AnyPacker;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.commandservice.customer.Customer;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("SubscriptionRecord should")
class SubscriptionRecordTest {

    @Test
    @DisplayName("match record to given parameters")
    void matchRecordToParams() {
        final SubscriptionRecord matchingRecord = new SubscriptionRecord(Given.subscription(),
                                                                         Given.target(),
                                                                         Given.TYPE);
        final Project entityState = Project.getDefaultInstance();
        final Any wrappedState = AnyPacker.pack(entityState);
        final ProjectId redundantId = ProjectId.getDefaultInstance();

        final boolean matchResult = matchingRecord.matches(Given.TYPE, redundantId, wrappedState);
        assertTrue(matchResult);
    }

    @Test
    @DisplayName("fail to match improper type")
    void notMatchImproperType() {
        final SubscriptionRecord notMatchingRecord = new SubscriptionRecord(Given.subscription(),
                                                                            Given.target(),
                                                                            Given.TYPE);
        final Project entityState = Project.getDefaultInstance();
        final Any wrappedState = AnyPacker.pack(entityState);
        final ProjectId redundantId = ProjectId.getDefaultInstance();

        final boolean matchResult = notMatchingRecord.matches(Given.OTHER_TYPE, redundantId, wrappedState);
        assertFalse(matchResult);
    }

    @Test
    @DisplayName("fail to match improper target")
    void notMatchImproperTarget() {
        final ProjectId nonExistingId = ProjectId.newBuilder()
                                                 .setId("never-existed")
                                                 .build();
        final SubscriptionRecord notMatchingRecord = new SubscriptionRecord(Given.subscription(),
                                                                            Given.target(nonExistingId),
                                                                            Given.TYPE);
        final Project entityState = Project.getDefaultInstance();
        final Any wrappedState = AnyPacker.pack(entityState);
        final ProjectId redundantId = ProjectId.getDefaultInstance();

        final boolean matchResult = notMatchingRecord.matches(Given.TYPE, redundantId, wrappedState);
        assertFalse(matchResult);
    }

    @Test
    @DisplayName("be equal only to SubscriptionRecord that has same subscription")
    void beEqualToSame() {
        final Subscription oneSubscription = Given.subscription();
        final SubscriptionId breakingId = Subscriptions.newId("breaking-id");
        final Subscription otherSubscription = Subscription.newBuilder()
                                                           .setId(breakingId)
                                                           .build();
        @SuppressWarnings("QuestionableName")
        final SubscriptionRecord one = new SubscriptionRecord(oneSubscription,
                                                              Given.target(),
                                                              Given.TYPE);
        final SubscriptionRecord similar = new SubscriptionRecord(otherSubscription,
                                                                  Given.target(),
                                                                  Given.TYPE);
        final SubscriptionRecord same = new SubscriptionRecord(oneSubscription,
                                                               Given.target(),
                                                               Given.TYPE);
        assertNotEquals(one, similar);
        assertEquals(one, same);
    }

    @SuppressWarnings("UtilityClass")
    private static class Given {

        private static final TypeUrl TYPE = TypeUrl.of(Project.class);
        private static final TypeUrl OTHER_TYPE = TypeUrl.of(Customer.class);

        private static Target target() {
            final Target target = Targets.allOf(Project.class);
            return target;
        }

        private static Target target(Message targetId) {
            final Target target = Targets.someOf(Project.class, Collections.singleton(targetId));
            return target;
        }

        private static Subscription subscription() {
            final Subscription subscription = Subscription.getDefaultInstance();
            return subscription;
        }
    }
}

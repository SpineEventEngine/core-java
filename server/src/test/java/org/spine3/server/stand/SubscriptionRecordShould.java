/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.stand;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.base.Queries;
import org.spine3.client.Subscription;
import org.spine3.client.Target;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.TypeUrl;
import org.spine3.test.aggregate.Project;
import org.spine3.test.aggregate.ProjectId;
import org.spine3.test.commandservice.customer.Customer;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
public class SubscriptionRecordShould {

    @Test
    public void match_record_to_given_parameters() {
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
    public void fail_to_match_improper_type() {
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
    public void fail_to_match_improper_target() {
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
    public void be_equal_if_has_same_subscription() {
        final Subscription oneSubscription = Given.subscription();
        final Subscription otherSubscription = Subscription.newBuilder()
                                                           .setId("breaking-id")
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
        assertFalse(one.equals(similar));
        assertTrue(one.equals(same));
    }

    @SuppressWarnings("UtilityClass")
    private static class Given {

        private static final TypeUrl TYPE = TypeUrl.of(Project.class);
        private static final TypeUrl OTHER_TYPE = TypeUrl.of(Customer.class);

        private static Target target() {
            final Target target = Queries.Targets.allOf(Project.class);
            return target;
        }

        private static Target target(Message targetId) {
            final Target target = Queries.Targets.someOf(Project.class, Collections.singleton(targetId));
            return target;
        }

        private static Subscription subscription() {
            final Subscription subscription = Subscription.getDefaultInstance();
            return subscription;
        }
    }
}

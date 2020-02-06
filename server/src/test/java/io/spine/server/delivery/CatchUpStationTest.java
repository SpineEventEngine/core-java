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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.base.Time;
import io.spine.server.delivery.given.TestCatchUpJobs;
import io.spine.server.delivery.given.TestInboxMessages;
import io.spine.test.delivery.DCounter;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.delivery.given.TestInboxMessages.copyWithNewId;
import static io.spine.server.delivery.given.TestInboxMessages.toDeliver;

@DisplayName("`CatchUpStation` should")
class CatchUpStationTest {

    @Test
    @DisplayName("do nothing if an empty conveyor is passed")
    void doNothingIfNoJobMatches() {
        MemoizingDeliveryAction action = new MemoizingDeliveryAction();
        CatchUpStation station = new CatchUpStation(action, new ArrayList<>());
        Conveyor emptyConveyor = new Conveyor(new ArrayList<>(), new DeliveredMessages());

        Station.Result result = station.process(emptyConveyor);
        assertThat(result.deliveredCount()).isEqualTo(0);
        assertThat(result.errors()
                         .hasErrors()).isFalse();

        assertThat(action.passedMessages()).isNull();
    }

    @Test
    @DisplayName("remove live messages which correspond to a started `CatchUpJob`")
    void removeLiveMessagesIfCatchUpStarted() {
        TypeUrl targetType = TypeUrl.of(DCounter.class);
        String targetId = "target";
        String anotherTarget = "another-target";
        InboxMessage toDeliver = toDeliver(targetId, targetType);
        InboxMessage anotherToDeliver = copyWithNewId(toDeliver);
        InboxMessage delivered = TestInboxMessages.delivered(targetId, targetType);
        InboxMessage differentTarget = TestInboxMessages.delivered(anotherTarget, targetType);
        Conveyor conveyor = new Conveyor(
                ImmutableList.of(toDeliver, anotherToDeliver, delivered, differentTarget),
                new DeliveredMessages()
        );

        MemoizingDeliveryAction action = new MemoizingDeliveryAction();
        CatchUp job = TestCatchUpJobs
                .catchUpJob(targetType, CatchUpStatus.STARTED,
                            Time.currentTime(), ImmutableList.of(targetId)
                );
        CatchUpStation station = new CatchUpStation(action, ImmutableList.of(job));
        Station.Result result = station.process(conveyor);

        assertThat(result.deliveredCount()).isEqualTo(0);
        assertThat(result.errors()
                         .hasErrors()).isFalse();

        Iterator<InboxMessage> removals = conveyor.removals();
        assertThat(ImmutableSet.copyOf(removals))
                .containsExactlyElementsIn(ImmutableSet.of(toDeliver, anotherToDeliver));

        Iterator<InboxMessage> remainders = conveyor.iterator();
        assertThat(ImmutableSet.copyOf(remainders))
                .containsExactlyElementsIn(ImmutableSet.of(delivered, differentTarget));
    }

    @Test
    @DisplayName("deliver the messages in `CATCH_UP` status " +
            "which correspond to a started `CatchUpJob`")
    void matchAndRunDeliveryAction() {

    }

    @Test
    @DisplayName("mark as `CATCH_UP` those live messages " +
            "which correspond to a finalizing `CatchUpJob`")
    void markLiveMessagesCatchUpIfJobFinalizing() {

    }

    @Test
    @DisplayName("de-duplicate and deliver all matching messages " +
            "if the respective `CatchUpJob` is completed, " +
            "keeping the `CATCH_UP` messages in their storage for a bit longer")
    void deduplicateAndDeliveryWhenJobCompleted() {

    }

    @Test
    @DisplayName("run the delivery action " +
            "for the messages in `CATCH_UP` status sorting them beforehand")
    void sortCatchUpMessagesBeforeCallingToAction() {

    }

    @Nested
    @DisplayName("match the `InboxMessage` to a `CatchUpJob`")
    class MatchCatchUpJob {

        @Test
        @DisplayName("by a particular target ID of the `CatchUpJob`")
        void byId() {

        }

        @Test
        @DisplayName("when the `CatchUpJob` matches all the targets of type " +
                "to which `InboxMessage` is dispatched")
        void allByType() {

        }
    }

    @Nested
    @DisplayName("not match the `InboxMessage` to a `CatchUpJob`")
    class NotMatchCatchUpJob {

        @Test
        @DisplayName("when the target type of the `InboxMessage` and the `CatchUpJob` differs")
        void whenTargetTypeDiffers() {

        }

        @Test
        @DisplayName("when the target ID of the `InboxMessage` " +
                "does not match the IDs enumerated in the `CatchUpJob`")
        void whenIdDoesNotMatch() {

        }
    }

    private static final class MemoizingDeliveryAction implements DeliveryAction {

        private @Nullable Collection<InboxMessage> passedMessages;

        @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")   // intentionally.
        @Override
        public DeliveryErrors executeFor(Collection<InboxMessage> messages) {
            passedMessages = messages;
            return DeliveryErrors.newBuilder()
                                 .build();
        }

        private @Nullable Collection<InboxMessage> passedMessages() {
            return passedMessages;
        }
    }

}

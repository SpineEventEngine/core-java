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
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.Durations.fromSeconds;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.base.Time.currentTime;
import static io.spine.server.delivery.InboxMessageStatus.DELIVERED;
import static io.spine.server.delivery.InboxMessageStatus.TO_DELIVER;
import static io.spine.server.delivery.given.TestInboxMessages.catchingUp;
import static io.spine.server.delivery.given.TestInboxMessages.copyWithNewId;
import static io.spine.server.delivery.given.TestInboxMessages.copyWithStatus;
import static io.spine.server.delivery.given.TestInboxMessages.delivered;
import static io.spine.server.delivery.given.TestInboxMessages.toDeliver;
import static java.util.stream.Collectors.toMap;

@DisplayName("`LiveDeliveryStation` should")
class LiveDeliveryStationTest extends AbstractStationTest {

    @Override
    Station newStation(DeliveryAction action) {
        return new LiveDeliveryStation(action, noWindow());
    }

    @Test
    @DisplayName("run the delivery action for all the messages in `TO_DELIVER` status")
    void runDeliveryActionForToDeliver() {
        InboxMessage toDeliver = toDeliver(targetOne, type);
        InboxMessage anotherToDeliver = toDeliver(targetOne, type);
        InboxMessage differentTarget = toDeliver(targetTwo, type);
        InboxMessage alreadyDelivered = delivered(targetOne, type);
        InboxMessage toCatchUp = catchingUp(targetOne, type);

        ImmutableList<InboxMessage> initialContents =
                ImmutableList.of(toDeliver, anotherToDeliver, differentTarget,
                                 alreadyDelivered, toCatchUp);
        Conveyor conveyor = new Conveyor(initialContents, new DeliveredMessages());

        MemoizingAction action = MemoizingAction.empty();
        Station station = new LiveDeliveryStation(action, noWindow());
        Station.Result result = station.process(conveyor);

        assertDeliveredCount(result, 3);
        List<InboxMessage> passedToAction = checkNotNull(action.passedMessages());

        assertContainsExactly(passedToAction,
                              // expected:
                              toDeliver,
                              anotherToDeliver,
                              differentTarget);

        assertContainsExactly(conveyor.iterator(),
                              // expected:
                              copyWithStatus(toDeliver, DELIVERED),
                              copyWithStatus(anotherToDeliver, DELIVERED),
                              copyWithStatus(differentTarget, DELIVERED),
                              alreadyDelivered,
                              toCatchUp);
    }

    @Test
    @DisplayName("remove duplicates prior to dispatching")
    void removeDuplicates() {
        InboxMessage toDeliver = toDeliver(targetOne, type);
        InboxMessage duplicate = copyWithNewId(toDeliver);
        InboxMessage anotherDuplicate = copyWithNewId(toDeliver);
        InboxMessage alreadyDelivered = delivered(targetOne, type);
        InboxMessage duplicateOfDelivered =
                copyWithNewId(copyWithStatus(alreadyDelivered, TO_DELIVER));
        InboxMessage toCatchUp = catchingUp(targetOne, type);

        ImmutableList<InboxMessage> initialContents =
                ImmutableList.of(toDeliver, duplicate, anotherDuplicate,
                                 alreadyDelivered, duplicateOfDelivered, toCatchUp);
        Conveyor conveyor = new Conveyor(initialContents, new DeliveredMessages());

        MemoizingAction action = MemoizingAction.empty();
        Station station = new LiveDeliveryStation(action, noWindow());
        Station.Result result = station.process(conveyor);

        assertDeliveredCount(result, 1);
        List<InboxMessage> passedToAction = checkNotNull(action.passedMessages());

        assertContainsExactly(passedToAction,
                              // expected:
                              toDeliver);

        assertContainsExactly(conveyor.removals(),
                              // expected:
                              duplicate,
                              anotherDuplicate,
                              duplicateOfDelivered);
        assertContainsExactly(conveyor.iterator(),
                              // expected:
                              copyWithStatus(toDeliver, DELIVERED),
                              alreadyDelivered,
                              toCatchUp);
    }

    @Test
    @DisplayName("sort messages prior to dispatching")
    void sort() {
        Timestamp now = currentTime();
        Timestamp secondBefore = subtract(now, fromSeconds(1));
        Timestamp twoSecondsBefore = subtract(now, fromSeconds(2));
        Timestamp threeSecondsBefore = subtract(now, fromSeconds(3));

        InboxMessage toDeliver1 = toDeliver(targetOne, type, threeSecondsBefore);
        InboxMessage toDeliver2 = toDeliver(targetTwo, type, twoSecondsBefore);
        InboxMessage toDeliver3 = toDeliver(targetOne, type, secondBefore);
        InboxMessage toDeliver4 = toDeliver(targetTwo, type, now);
        Conveyor conveyor = new Conveyor(
                ImmutableList.of(toDeliver2, toDeliver3, toDeliver4, toDeliver1),
                new DeliveredMessages()
        );

        MemoizingAction action = MemoizingAction.empty();
        Station station = new LiveDeliveryStation(action, noWindow());
        Station.Result result = station.process(conveyor);
        assertDeliveredCount(result, 4);

        List<InboxMessage> deliveredMessages = checkNotNull(action.passedMessages());
        assertThat(deliveredMessages).containsExactlyElementsIn(
                ImmutableList.of(toDeliver1, toDeliver2, toDeliver3, toDeliver4)
        );
    }

    @Test
    @DisplayName("modify the messages and keep them for longer if the deduplication window is set")
    void keepMessagesForLongerIfDeduplicationWindowSet() {
        InboxMessage toDeliver = toDeliver(targetOne, type);
        InboxMessage differentTarget = toDeliver(targetTwo, type);
        InboxMessage alreadyDelivered = delivered(targetOne, type);
        InboxMessage toCatchUp = catchingUp(targetOne, type);

        ImmutableList<InboxMessage> initialContents =
                ImmutableList.of(toDeliver, differentTarget, alreadyDelivered, toCatchUp);
        Conveyor conveyor = new Conveyor(initialContents, new DeliveredMessages());

        Station station = new LiveDeliveryStation(MemoizingAction.empty(), fromSeconds(100));
        Station.Result result = station.process(conveyor);

        assertDeliveredCount(result, 2);

        Map<InboxMessageId, InboxMessage> contentsById =
                stream(conveyor.iterator()).collect(toMap(InboxMessage::getId,msg -> msg));
        assertKeptForLonger(toDeliver.getId(), contentsById);
        assertKeptForLonger(differentTarget.getId(), contentsById);
        assertNotKeptForLonger(alreadyDelivered.getId(), contentsById);
        assertNotKeptForLonger(toCatchUp.getId(), contentsById);
    }

    private static Duration noWindow() {
        return fromSeconds(0);
    }
}

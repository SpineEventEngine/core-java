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

package io.spine.server.event;

import com.google.common.testing.EqualsTester;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.client.Queries;
import io.spine.core.Event;
import io.spine.server.aggregate.AggregateStateRecord;
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.FieldMasks;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
public class EventStreamShould {

    private static final TestEventFactory factory = TestEventFactory.newInstance(
            EventStreamShould.class
    );

    @Test
    public void keep_events_order() {
        final EventStream stream = EventStream.newBuilder()
                                              .add(TestEvent.FIRST.value)
                                              .add(TestEvent.SECOND.value)
                                              .add(TestEvent.THIRD.value)
                                              .add(TestEvent.FORTH.value)
                                              .build();
        assertThat(stream.events(), contains(TestEvent.FIRST.value,
                                             TestEvent.SECOND.value,
                                             TestEvent.THIRD.value,
                                             TestEvent.FORTH.value));
    }

    @Test
    public void have_equals_and_hashcode() {
        new EqualsTester()
                .addEqualityGroup(EventStream.of(TestEvent.FIRST.value, TestEvent.SECOND.value),
                                  EventStream.newBuilder()
                                             .add(TestEvent.FIRST.value)
                                             .add(TestEvent.SECOND.value)
                                             .build(),
                                  EventStream.of(TestEvent.FIRST.value)
                                             .concat(EventStream.of(TestEvent.SECOND.value)))
                .addEqualityGroup(EventStream.of(),
                                  EventStream.empty(),
                                  EventStream.newBuilder().build(),
                                  EventStream.from(emptyList()),
                                  EventStream.from(AggregateStateRecord.getDefaultInstance()))
                .addEqualityGroup(EventStream.from(
                        AggregateStateRecord.newBuilder()
                                            .addEvent(TestEvent.THIRD.value)
                                            .build()),
                                  EventStream.of(TestEvent.THIRD.value))
                .testEquals();
    }

    @Test
    public void report_count() {
        final EventStream streamOfTwo = EventStream.of(TestEvent.FIRST.value,
                                                       TestEvent.SECOND.value);
        assertFalse(streamOfTwo.isEmpty());
        assertEquals(2, streamOfTwo.count());
    }

    @Test
    public void report_if_empty() {
        final EventStream stream = EventStream.empty();
        assertTrue(stream.isEmpty());
    }

    @Test
    public void concat_two_non_empty_streams() {
        final EventStream prefix = EventStream.of(TestEvent.FIRST.value, TestEvent.SECOND.value);
        final EventStream suffix = EventStream.of(TestEvent.THIRD.value, TestEvent.FORTH.value);

        final EventStream together = prefix.concat(suffix);
        assertThat(together.events(), contains(TestEvent.FIRST.value,
                                               TestEvent.SECOND.value,
                                               TestEvent.THIRD.value,
                                               TestEvent.FORTH.value));
    }

    @Test
    public void concat_with_empty_stream() {
        final EventStream nonEmpty = EventStream.of(TestEvent.FORTH.value);
        final EventStream empty = EventStream.empty();

        assertFalse(nonEmpty.isEmpty());
        assertTrue(empty.isEmpty());

        final EventStream concatenated = nonEmpty.concat(empty);
        assertEquals(nonEmpty, concatenated);
    }

    @Test
    public void concat_empty_with_nonempty_stream() {
        final EventStream empty = EventStream.empty();
        final EventStream nonEmpty = EventStream.of(TestEvent.SECOND.value);

        assertTrue(empty.isEmpty());
        assertFalse(nonEmpty.isEmpty());

        final EventStream concatenated = empty.concat(nonEmpty);
        assertEquals(nonEmpty, concatenated);
    }

    private enum TestEvent {

        FIRST(Time.getCurrentTime()),
        SECOND(Time.systemTime()),
        THIRD(FieldMasks.maskOf(Timestamp.getDescriptor(), Timestamp.SECONDS_FIELD_NUMBER)),
        FORTH(Queries.generateId());

        private final Event value;

        TestEvent(Message message) {
            this.value = factory.createEvent(message);
        }
    }
}

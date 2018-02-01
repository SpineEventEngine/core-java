/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.bus;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Rejection;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.bus.given.BusesTestEnv.Exceptions.ErrorType;
import io.spine.server.bus.given.BusesTestEnv.Filters.ContentsFlagFilter;
import io.spine.server.bus.given.BusesTestEnv.Filters.FailingFilter;
import io.spine.server.bus.given.BusesTestEnv.Filters.PassingFilter;
import io.spine.server.bus.given.BusesTestEnv.TestMessageBus;
import io.spine.test.bus.BusMessage;
import io.spine.test.bus.TestMessageContents;
import org.junit.Test;

import java.util.List;

import static io.spine.grpc.StreamObservers.memoizingObserver;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.bus.given.BusesTestEnv.STATUS_OK;
import static io.spine.server.bus.given.BusesTestEnv.busBuilder;
import static io.spine.server.bus.given.BusesTestEnv.busMessage;
import static io.spine.server.bus.given.BusesTestEnv.errorType;
import static io.spine.server.bus.given.BusesTestEnv.testContents;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertSize;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
public class BusesShould {

    @Test
    public void have_private_util_ctor() {
        assertHasPrivateParameterlessCtor(Buses.class);
    }

    @Test
    public void not_accept_nulls() {
        new NullPointerTester()
                .setDefault(Message.class, Any.getDefaultInstance())
                .setDefault(Error.class, Error.newBuilder()
                                              .setCode(1)
                                              .build())
                .setDefault(Rejection.class, Rejection.newBuilder()
                                                      .setMessage(Any.getDefaultInstance())
                                                      .build())
                .testAllPublicStaticMethods(Buses.class);
    }

    @Test
    public void acknowledged_valid_message_delivery() {
        final TestMessageBus bus = busBuilder().build();
        final BusMessage message = busMessage(testContents());
        final MemoizingObserver<Ack> observer = memoizingObserver();

        bus.post(message, observer);

        final List<Ack> responses = observer.responses();
        assertSize(1, responses);

        final Ack response = responses.get(0);
        assertEquals(STATUS_OK, response.getStatus());
    }

    @Test
    public void apply_the_validating_filter() {
        final TestMessageBus bus = busBuilder().failingValidation()
                                               .build();

        assertBusPostErrs(bus, ErrorType.FAILING_VALIDATION);
    }

    @Test
    public void apply_the_dead_message_filter() {
        final TestMessageBus bus = busBuilder().withNoDispatchers()
                                               .build();

        assertBusPostErrs(bus, ErrorType.DEAD_MESSAGE);
    }

    @Test
    public void apply_a_registered_filter() {
        final TestMessageBus bus = busBuilder().addFilter(new FailingFilter())
                                               .build();

        assertBusPostErrs(bus, ErrorType.FAILING_FILTER);
    }

    @Test
    public void apply_registered_filters() {
        final PassingFilter passingFilter = new PassingFilter();
        final PassingFilter passingFilter2 = new PassingFilter();

        final TestMessageBus bus = busBuilder().addFilter(passingFilter)
                                               .addFilter(passingFilter2)
                                               .addFilter(new FailingFilter())
                                               .build();

        assertBusPostErrs(bus, ErrorType.FAILING_FILTER);

        assertTrue(passingFilter.passed());
        assertTrue(passingFilter2.passed());
    }

    /**
     * Asserts that bus acknowledges the error when posting a single message.
     */
    private static void assertBusPostErrs(TestMessageBus bus, ErrorType type) {
        final BusMessage message = busMessage(testContents());
        final MemoizingObserver<Ack> observer = memoizingObserver();

        bus.post(message, observer);

        final List<Ack> responses = observer.responses();
        assertSize(1, responses);

        final Ack response = responses.get(0);
        assertEquals(type.toString(), errorType(response));
        assertSize(0, bus.storedMessages());
    }

    @Test
    public void store_only_messages_passing_filters() {
        final TestMessageBus bus = busBuilder().addFilter(new ContentsFlagFilter())
                                               .build();
        final BusMessage message = busMessage(testContents(true));
        final BusMessage message2 = busMessage(testContents(false));
        final BusMessage message3 = busMessage(testContents(true));
        final BusMessage message4 = busMessage(testContents(false));
        final BusMessage message5 = busMessage(testContents(false));

        final List<BusMessage> messages = asList(message, message2, message3, message4, message5);
        final MemoizingObserver<Ack> observer = memoizingObserver();

        bus.post(messages, observer);

        final List<Ack> responses = observer.responses();
        assertSize(5, responses);

        final List<BusMessage> storedMessages = bus.storedMessages();
        assertSize(2, storedMessages);

        for (BusMessage storedMessage : storedMessages) {
            final TestMessageContents contents = unpack(storedMessage.getContents());
            assertTrue(contents.getFlag());
        }
    }

    @Test
    public void not_store_any_messages_when_they_are_failing_filtering() {
        final TestMessageBus bus = busBuilder().addFilter(new ContentsFlagFilter())
                                               .build();
        final BusMessage message = busMessage(testContents(false));
        final BusMessage message2 = busMessage(testContents(false));
        final BusMessage message3 = busMessage(testContents(false));

        final List<BusMessage> messages = asList(message, message2, message3);
        final MemoizingObserver<Ack> observer = memoizingObserver();

        bus.post(messages, observer);

        final List<Ack> responses = observer.responses();
        assertSize(3, responses);
        assertSize(0, bus.storedMessages());
    }

}

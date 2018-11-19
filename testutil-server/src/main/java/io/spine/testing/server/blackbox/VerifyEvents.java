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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.testing.client.blackbox.Count;

import static io.spine.testing.server.blackbox.AbstractVerify.classes;
import static io.spine.testing.server.blackbox.AbstractVerify.count;
import static io.spine.testing.server.blackbox.AbstractVerify.countAndClass;

/**
 * Verifies that a {@link BlackBoxBoundedContext Bounded Context} emitted events that satisfy
 * criteria defined by a factory method that returns an instance of this class.
 *
 * @author Mykhailo Drachuk
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class VerifyEvents extends DelegatingVerify<EmittedEvents> {

    private VerifyEvents(VerifyMessages<EmittedEvents> delegate) {
        super(delegate);
    }

    /**
     * Verifies that there was a specific number of events of the provided class emitted
     * in the Bounded Context.
     */
    public static VerifyEvents emittedEvent(Class<? extends Message> eventClass, Count expected) {
        return new VerifyEvents(countAndClass(expected, eventClass));
    }

    /**
     * Verifies that there was an expected amount of events of any classes emitted
     * in the Bounded Context.
     */
    public static VerifyEvents emittedEvent(Count expected) {
        return new VerifyEvents(count(expected));
    }

    /**
     * Verifies that there were events of each of the provided event classes.
     */
    @SafeVarargs
    public static VerifyEvents emittedEvents(Class<? extends Message> firstEventClass,
                                             Class<? extends Message>... otherEventClasses) {
        return new VerifyEvents(classes(firstEventClass, otherEventClasses));
    }

    public static VerifyEvents emittedEventsHadVersions(int... versions) {
        return new VerifyEvents(new EventVersionsVerifier(versions));
    }
}

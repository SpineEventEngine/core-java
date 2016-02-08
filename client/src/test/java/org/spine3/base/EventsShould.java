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
package org.spine3.base;

import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Timestamps.secondsAgo;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings("InstanceMethodNamingConvention")
public class EventsShould {

    @Test
    public void have_private_ctor() {
        assertTrue(hasPrivateUtilityConstructor(Events.class));
    }

    @Test
    public void generate_event_id() {
        final EventId result = Events.generateId();

        assertFalse(result.getUuid().isEmpty());
    }

    @Test
    public void return_null_from_null_input_in_IsAfter_predicate() {
        assertFalse(new Events.IsAfter(secondsAgo(5)).apply(null));
    }

    @Test
    public void return_null_from_null_input_in_IsBefore_predicate() {
        assertFalse(new Events.IsBefore(secondsAgo(5)).apply(null));
    }

    @Test
    public void return_null_from_null_input_in_IsBetween_predicate() {
        assertFalse(new Events.IsBetween(secondsAgo(5), secondsAgo(1)).apply(null));
    }

    @Test
    public void return_actor_from_EventContext() {
        // Since Events.getActor() is merely wrapper over the chain of generated method calls
        // the main reason to have this test is to mark the Events.getActor() as `used`.
        checkNotNull(Events.getActor(EventContext.getDefaultInstance()));
    }
}

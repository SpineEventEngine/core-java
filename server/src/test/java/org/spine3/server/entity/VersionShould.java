/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity;

import com.google.protobuf.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.protobuf.Timestamps;
import org.spine3.test.Tests;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class VersionShould {

    private static final Timestamp currentTime = Timestamps.systemTime();

    @Before
    public void mockCurrentTime() {
        Timestamps.setProvider(new Tests.FrozenMadHatterParty(currentTime));
    }

    @After
    public void backToNormalTime() {
        Timestamps.resetProvider();
    }

    @Test
    public void create_zero_instance() {
        final Version version = Version.create();
        assertEquals(0, version.getNumber());
        assertEquals(currentTime, version.getTimestamp());
    }

    @Test
    public void create_instance_by_values() throws Exception {
        final int number = 42;
        final Timestamp timestamp = Timestamps.systemTime();

        final Version version = Version.of(number, timestamp);

        assertEquals(number, version.getNumber());
        assertEquals(timestamp, version.getTimestamp());
    }

    @Test
    public void increment() throws Exception {
        final int number = 100500;
        final Timestamp timestamp = Timestamps.systemTime();
        final Version version = Version.of(number, timestamp);

        // Check that we have timestamp we passed.
        assertEquals(timestamp, version.getTimestamp());

        version.increment();

        assertEquals(number + 1, version.getNumber());

        // Check that the new timestamp
        assertEquals(currentTime, version.getTimestamp());
    }
}

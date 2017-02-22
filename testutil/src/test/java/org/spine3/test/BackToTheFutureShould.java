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

package org.spine3.test;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.TimeTests.BackToTheFuture.THIRTY_YEARS_IN_HOURS;

/**
 * @author Alexander Yevsyukov
 */
public class BackToTheFutureShould {

    private TimeTests.BackToTheFuture timestampProvider;

    @Before
    public void setUp() {
       timestampProvider = new TimeTests.BackToTheFuture();
    }
    @Test
    public void create_with_start_in_the_future() {
        assertTrue(TimeTests.Future.isFuture(timestampProvider.getCurrentTime()));
    }

    @Test
    public void rewind_backward() {
        // Rewind to somewhere around present.
        timestampProvider.backward(THIRTY_YEARS_IN_HOURS);

        // ... and back to 30 years in the past.
        timestampProvider.backward(THIRTY_YEARS_IN_HOURS);

        assertFalse(TimeTests.Future.isFuture(timestampProvider.getCurrentTime()));
    }

    @SuppressWarnings("MagicNumber")
    @Test
    public void rewind_forward() {
        // Rewind to somewhere around present.
        timestampProvider.backward(THIRTY_YEARS_IN_HOURS);

        timestampProvider.forward(THIRTY_YEARS_IN_HOURS + 24L);

        assertTrue(TimeTests.Future.isFuture(timestampProvider.getCurrentTime()));
    }
}

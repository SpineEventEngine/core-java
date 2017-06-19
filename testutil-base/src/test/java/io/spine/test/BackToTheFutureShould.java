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

package io.spine.test;

import io.spine.test.TimeTests.BackToTheFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class BackToTheFutureShould {

    private BackToTheFuture timeProvider;

    @Before
    public void setUp() {
       timeProvider = new BackToTheFuture();
    }
    @Test
    public void create_with_start_in_the_future() {
        assertTrue(TimeTests.Future.isFuture(timeProvider.getCurrentTime()));
    }

    @Test
    public void rewind_backward() {
        // Rewind to somewhere around present.
        Assert.assertNotEquals(timeProvider.getCurrentTime(),
                               timeProvider.backward(BackToTheFuture.THIRTY_YEARS_IN_HOURS));

        // ... and back to 30 years in the past.
        timeProvider.backward(BackToTheFuture.THIRTY_YEARS_IN_HOURS);

        assertFalse(TimeTests.Future.isFuture(timeProvider.getCurrentTime()));
    }

    @SuppressWarnings("MagicNumber")
    @Test
    public void rewind_forward() {
        // Rewind to somewhere around present.
        timeProvider.backward(BackToTheFuture.THIRTY_YEARS_IN_HOURS);

        timeProvider.forward(BackToTheFuture.THIRTY_YEARS_IN_HOURS + 24L);

        assertTrue(TimeTests.Future.isFuture(timeProvider.getCurrentTime()));
    }
}

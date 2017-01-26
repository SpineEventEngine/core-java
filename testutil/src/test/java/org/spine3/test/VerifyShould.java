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

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class VerifyShould {

    @Test
    public void extend_Assert_class() {
        final Type expectedSuperclass = Assert.class;
        final Type actualSuperclass = Verify.class.getGenericSuperclass();
        assertEquals(expectedSuperclass, actualSuperclass);
    }

    @Test
    public void have_private_ctor() {
        assertTrue(Tests.hasPrivateUtilityConstructor(Verify.class));
    }

    @SuppressWarnings({"ThrowCaughtLocally", "ErrorNotRethrown"})
    @Test
    public void mangle_assertion_error() {
        final AssertionError sourceError = new AssertionError();
        final int framesBefore = sourceError.getStackTrace().length;

        try {
            throw Verify.mangledException(sourceError);
        } catch (AssertionError e) {
            final int framesAfter = e.getStackTrace().length;

            assertEquals(framesBefore - 1, framesAfter);
        }
    }

    @SuppressWarnings({"ThrowCaughtLocally", "ErrorNotRethrown"})
    @Test
    public void mangle_assertion_error_for_specified_frame_count() {
        final AssertionError sourceError = new AssertionError();
        final int framesBefore = sourceError.getStackTrace().length;
        final int framesToPop = 3;

        try {
            throw Verify.mangledException(sourceError, framesToPop);
        } catch (AssertionError e) {
            final int framesAfter = e.getStackTrace().length;

            assertEquals(framesBefore - framesToPop + 1, framesAfter);
        }
    }

    @SuppressWarnings("ErrorNotRethrown")
    @Test
    public void fail_with_specified_message_and_cause() {
        final String message = "Test failed";
        final Throwable cause = new Error();

        try {
            Verify.fail(message, cause);
            fail("Error was not thrown");
        } catch (AssertionError e) {
            assertEquals(message, e.getMessage());
            assertEquals(cause, e.getCause());
        }
    }

}

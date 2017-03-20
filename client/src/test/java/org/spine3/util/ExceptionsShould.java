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

package org.spine3.util;

import com.google.common.testing.NullPointerTester;
import org.junit.Test;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;
import static org.spine3.util.Exceptions.newIllegalArgumentException;
import static org.spine3.util.Exceptions.newIllegalStateException;
import static org.spine3.util.Exceptions.unsupported;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class ExceptionsShould {

    @Test
    public void have_private_ctor() {
        assertHasPrivateParameterlessCtor(Exceptions.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void create_and_throw_unsupported_operation_exception() {
        unsupported();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void create_and_throw_unsupported_operation_exception_with_message() {
        unsupported(newUuid());
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Exception.class, new RuntimeException(""))
                .setDefault(Throwable.class, new Error())
                .testAllPublicStaticMethods(Exceptions.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_formatted_IAE() {
        newIllegalArgumentException("%d, %d, %s kaboom", 1, 2, "three");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_formatted_IAE_with_cause() {
        newIllegalArgumentException(new RuntimeException("checking"), "%s", "stuff");
    }

    @Test(expected = IllegalStateException.class)
    public void throw_formatted_ISE() {
        newIllegalStateException("%s check %s", "state", "failed");
    }

    @Test(expected = IllegalStateException.class)
    public void throw_formatted_ISE_with_cause() {
        newIllegalStateException(new RuntimeException(getClass().getSimpleName()),
                                 "%s %s", "taram", "param");
    }
}

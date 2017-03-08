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

package org.spine3.base;

import com.google.common.testing.NullPointerTester;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsyukov
 */
public class FailuresShould {

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(Failures.class);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(Command.class, Command.getDefaultInstance())
                .testAllPublicStaticMethods(Failures.class);

    }

    @Test
    public void generate_failure_id() {
        assertFalse(Failures.generateId()
                            .getUuid()
                            .isEmpty());
    }
}

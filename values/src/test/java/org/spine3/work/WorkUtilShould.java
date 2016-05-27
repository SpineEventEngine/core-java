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

package org.spine3.work;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

/**
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class WorkUtilShould {

    private static final String CONVERSION_ERROR = "Work time conversion failed";

    @SuppressWarnings({"MagicNumber", "LocalVariableNamingConvention"})
    @Test
    public void return_valid_work_instance() {
        final Work fiveMinutesWork = WorkUtil.ofMinutes(5);
        assertEquals(CONVERSION_ERROR, 5, fiveMinutesWork.getMinutes());

        final Work twoHourWork = WorkUtil.ofHours(2);
        assertEquals(CONVERSION_ERROR, 120, twoHourWork.getMinutes());

        final Work fiveHoursAndTwelveMinutesWork = WorkUtil.ofHoursAndMinutes(5, 12);
        assertEquals(CONVERSION_ERROR, 312, fiveHoursAndTwelveMinutesWork.getMinutes());
    }

    @Test
    public void pass_on_zero_amount_of_work() {
        WorkUtil.ofMinutes(0);
        WorkUtil.ofHours(0);
        WorkUtil.ofHoursAndMinutes(0, 0);
    }

    @Test
    public void fail_on_negative_amount_of_time() {
        try {
            WorkUtil.ofMinutes(-1);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            WorkUtil.ofHours(-1);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            WorkUtil.ofHoursAndMinutes(-1, 0);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            WorkUtil.ofHoursAndMinutes(0, -1);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
        try {
            WorkUtil.ofHoursAndMinutes(-1, -1);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(WorkUtil.class));
    }

}

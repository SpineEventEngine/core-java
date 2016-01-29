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

package org.spine3.time;

import org.junit.Test;
import org.spine3.protobuf.Timestamps;
import org.spine3.util.Tests;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ZoneOffsetsShould {

    @SuppressWarnings("MethodWithTooExceptionsDeclared")
    @Test
    public void have_private_utility_ctor()
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Tests.callPrivateUtilityConstructor(ZoneOffsets.class);
    }

    @Test
    public void create_instance_by_hour_offset() {
        assertEquals(Timestamps.SECONDS_PER_MINUTE * Timestamps.MINUTES_PER_HOUR * 2,
                     ZoneOffsets.ofHours(2).getAmountSeconds());
    }
}

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

import org.spine3.protobuf.Durations;

import javax.xml.datatype.Duration;

/**
 *  Routines for working with {@link OffsetTime}.
 *
 * @author Alexander Aleksandrov
 */
public class OffsetTimes {

    private OffsetTimes() {
    }

    /**
     * Obtains current OffsetTime instance using {@link ZoneOffset}.
     */
    public static OffsetTime now(ZoneOffset zoneOffset) {
        final LocalTime localTime = LocalTimes.now();

        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(localTime)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }

    /**
     * Obtains OffsetTime instance using {@link LocalTime} and {@link ZoneOffset}.
     */
    public static OffsetTime of(LocalTime localTime, ZoneOffset zoneOffset) {

        final OffsetTime result = OffsetTime.newBuilder()
                                            .setTime(localTime)
                                            .setOffset(zoneOffset)
                                            .build();
        return result;
    }
    
}

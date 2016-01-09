/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.protobuf;

import com.google.protobuf.Timestamp;
import org.junit.Test;

import java.util.Date;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.assertEquals;
import static org.spine3.protobuf.Timestamps.MILLIS_PER_SECOND;
import static org.spine3.protobuf.Timestamps.convertToDate;

@SuppressWarnings("InstanceMethodNamingConvention")
public class TimestampsShould {

    @Test
    public void convert_timestamp_to_date_to_nearest_second() {

        final Timestamp expectedTime = getCurrentTime();

        final Date actualDate = convertToDate(expectedTime);
        final long actualSeconds = actualDate.getTime() / (long) MILLIS_PER_SECOND;

        assertEquals(expectedTime.getSeconds(), actualSeconds);
    }
}

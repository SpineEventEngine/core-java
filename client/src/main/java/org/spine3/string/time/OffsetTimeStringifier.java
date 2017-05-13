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

package org.spine3.string.time;

import org.spine3.string.Stringifier;
import org.spine3.time.OffsetTime;
import org.spine3.time.OffsetTimes;

import java.io.Serializable;
import java.text.ParseException;

import static org.spine3.util.Exceptions.illegalArgumentWithCauseOf;

/**
 * Default stringifier for {@link OffsetTime}.
 *
 * @author Alexander Yevsyukov
 */
final class OffsetTimeStringifier extends Stringifier<OffsetTime> implements Serializable {

    private static final long serialVersionUID = 0L;
    private static final OffsetTimeStringifier INSTANCE = new OffsetTimeStringifier();

    static OffsetTimeStringifier getInstance() {
        return INSTANCE;
    }

    @Override
    protected String toString(OffsetTime time) {
        final String result = OffsetTimes.toString(time);
        return result;
    }

    @Override
    protected OffsetTime fromString(String str) {
        final OffsetTime result;
        try {
            result = OffsetTimes.parse(str);
        } catch (ParseException e) {
            throw illegalArgumentWithCauseOf(e);
        }
        return result;
    }

    @Override
    public String toString() {
        return "TimeStringifiers.forOffsetTime()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}

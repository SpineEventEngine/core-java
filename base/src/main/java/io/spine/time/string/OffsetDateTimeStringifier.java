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

package io.spine.time.string;

import io.spine.string.Stringifier;
import io.spine.time.OffsetDateTime;
import io.spine.time.OffsetDateTimes;
import io.spine.util.Exceptions;

import java.io.Serializable;
import java.text.ParseException;

/**
 * Default stringifier for {@link OffsetDateTime}.
 *
 * @author Alexander Yevsyukov
 */
final class OffsetDateTimeStringifier extends Stringifier<OffsetDateTime> implements Serializable {

    private static final long serialVersionUID = 0L;
    private static final OffsetDateTimeStringifier INSTANCE = new OffsetDateTimeStringifier();

    static OffsetDateTimeStringifier getInstance() {
        return INSTANCE;
    }

    @Override
    protected String toString(OffsetDateTime dateTime) {
        final String result = OffsetDateTimes.toString(dateTime);
        return result;
    }

    @Override
    protected OffsetDateTime fromString(String str) {
        final OffsetDateTime result;
        try {
            result = OffsetDateTimes.parse(str);
        } catch (ParseException e) {
            throw Exceptions.illegalArgumentWithCauseOf(e);
        }
        return result;
    }

    @Override
    public String toString() {
        return "TimeStringifiers.forOffsetDateTime()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}

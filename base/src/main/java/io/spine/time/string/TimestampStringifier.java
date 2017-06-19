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

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.string.Stringifier;

import java.io.Serializable;
import java.text.ParseException;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * The stringifier of timestamps into RFC 3339 date string format.
 *
 * @author Alexander Yevsyukov
 */
final class TimestampStringifier extends Stringifier<Timestamp> implements Serializable {

    private static final long serialVersionUID = 0L;
    private static final TimestampStringifier INSTANCE = new TimestampStringifier();

    static TimestampStringifier getInstance() {
        return INSTANCE;
    }

    @Override
    protected String toString(Timestamp obj) {
        return Timestamps.toString(obj);
    }

    @Override
    @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
    // It is OK because all necessary information from caught exception is passed.
    protected Timestamp fromString(String str) {
        try {
            return Timestamps.parse(str);
        } catch (ParseException e) {
            throw newIllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "TimeStringifiers.forTimestamp()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}

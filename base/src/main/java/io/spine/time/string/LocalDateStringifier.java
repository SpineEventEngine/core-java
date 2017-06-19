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
import io.spine.time.LocalDate;
import io.spine.time.LocalDates;

import java.io.Serializable;
import java.text.ParseException;

import static io.spine.util.Exceptions.illegalArgumentWithCauseOf;

/**
 * The default stringifier for {@link LocalDate} instances.
 *
 * @author Alexander Yevsyukov
 */
final class LocalDateStringifier extends Stringifier<LocalDate> implements Serializable {

    private static final long serialVersionUID = 0L;
    private static final LocalDateStringifier INSTANCE = new LocalDateStringifier();

    static LocalDateStringifier getInstance() {
        return INSTANCE;
    }

    @Override
    protected String toString(LocalDate date) {
        final String result = LocalDates.toString(date);
        return result;
    }

    @Override
    protected LocalDate fromString(String str) {
        final LocalDate date;
        try {
            date = LocalDates.parse(str);
        } catch (ParseException e) {
            throw illegalArgumentWithCauseOf(e);
        }
        return date;
    }

    @Override
    public String toString() {
        return "TimeStringifiers.forLocalDate()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}

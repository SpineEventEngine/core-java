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
import org.spine3.time.LocalDate;
import org.spine3.time.LocalDates;

import java.io.Serializable;
import java.text.ParseException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * The default stringifier for {@link LocalDate} instances.
 *
 * @author Alexander Yevsyukov
 */
final class LocalDateStringifier extends Stringifier<LocalDate> implements Serializable {

    private static final long serialVersionUID = 1;
    private static final LocalDateStringifier INSTANCE = new LocalDateStringifier();

    static LocalDateStringifier instance() {
        return INSTANCE;
    }

    @Override
    protected String toString(LocalDate date) {
        checkNotNull(date);
        final String result = LocalDates.toString(date);
        return result;
    }

    @Override
    protected LocalDate fromString(String str) {
        checkNotNull(str);
        final LocalDate date;
        try {
            date = LocalDates.parse(str);
        } catch (ParseException e) {
            throw wrappedCause(e);
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

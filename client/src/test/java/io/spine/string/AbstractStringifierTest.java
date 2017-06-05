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

package io.spine.string;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The abstract base for stringifier tests.
 *
 * @author Alexander Yevsyukov
 * @param <T> the type of stringifier objects
 */
public abstract class AbstractStringifierTest<T> {

    private final Stringifier<T> stringifier;

    protected AbstractStringifierTest(Stringifier<T> stringifier) {
        this.stringifier = stringifier;
    }

    protected abstract T createObject();

    protected Stringifier<T> getStringifier() {
        return stringifier;
    }

    @Test
    public void convert() {
        T obj = createObject();

        final String str = stringifier.convert(obj);
        final T convertedBack = stringifier.reverse()
                                           .convert(str);

        assertEquals(obj, convertedBack);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_IAE_on_empty_string() {
        stringifier.reverse()
                   .convert("");
    }
}

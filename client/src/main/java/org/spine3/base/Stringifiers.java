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

package org.spine3.base;

import com.google.common.reflect.TypeToken;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.StringifierRegistry.getStringifier;

/**
 * Utility class for working with {@code Stringifier}s.
 *
 * @author Alexander Yevsyukov
 * @author Illia Shepilov
 */
public class Stringifiers {

    private Stringifiers() {
        // Disable instantiation of this utility class.
    }

    /**
     * Converts the passed value to the string representation.
     *
     * @param object    to object to convert
     * @param typeToken the type token of the passed value
     * @param <I>       the type of the object to convert
     * @return the string representation of the passed value
     * @throws org.spine3.validate.IllegalConversionArgumentException if passed value cannot be converted
     */
    public static <I> String toString(I object, TypeToken<I> typeToken) {
        checkNotNull(object);
        checkNotNull(typeToken);

        final Stringifier<I> stringifier = getStringifier(typeToken);
        final String result = stringifier.convert(object);
        return result;
    }

    /**
     * Parses string to the appropriate value.
     *
     * @param valueToParse value to convert
     * @param typeToken    the type token of the returned value
     * @param <I>          the type of the value to return
     * @return the parsed value from string
     * @throws org.spine3.validate.IllegalConversionArgumentException if passed string cannot be parsed
     */
    public static <I> I fromString(String valueToParse, TypeToken<I> typeToken) {
        checkNotNull(valueToParse);
        checkNotNull(typeToken);

        final Stringifier<I> stringifier = getStringifier(typeToken);
        final I result = stringifier.reverse()
                                    .convert(valueToParse);
        return result;
    }
}

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

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.spine3.util.Exceptions.conversionArgumentException;

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
     * @param valueToConvert value to convert
     * @param typeToken      the type token of the passed value
     * @param <I>            the type of the value to convert
     * @return the string representation of the passed value
     * @throws org.spine3.validate.IllegalConversionArgumentException if passed value cannot be converted
     */
    public static <I> String toString(I valueToConvert, TypeToken<I> typeToken) {
        checkNotNull(valueToConvert);
        checkNotNull(typeToken);

        final Stringifier<I> stringifier = getStringifier(typeToken);
        final String result = stringifier.convert(valueToConvert);
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
    public static <I> I parse(String valueToParse, TypeToken<I> typeToken) {
        checkNotNull(valueToParse);
        checkNotNull(typeToken);

        final Stringifier<I> stringifier = getStringifier(typeToken);
        final I result = stringifier.reverse()
                                    .convert(valueToParse);
        return result;
    }

    private static <I> Stringifier<I> getStringifier(TypeToken<I> typeToken) {
        final Optional<Stringifier<I>> stringifierOptional = StringifierRegistry.getInstance()
                                                                                .get(typeToken);

        if (!stringifierOptional.isPresent()) {
            final String exMessage =
                    format("Stringifier for the %s is not provided", typeToken);
            throw conversionArgumentException(exMessage);
        }
        final Stringifier<I> stringifier = stringifierOptional.get();
        return stringifier;
    }

}

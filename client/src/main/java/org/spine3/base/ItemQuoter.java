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

import com.google.common.base.Converter;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Exceptions.newIllegalArgumentException;

/**
 * Encloses and discloses the {@code String} objects with quotes.
 */
class ItemQuoter {

    private static final char QUOTE_SYMBOL = '\"';

    private ItemQuoter() {
        // Disable instantiation from the outside.
    }

    static Converter<String, String> converter() {
        return new QuotedItemConverter();
    }

    private static class QuotedItemConverter extends Converter<String, String> {

        @Override
        protected String doForward(String s) {
            return quoteElement(s);
        }

        private static String quoteElement(String stringToQuote) {
            checkNotNull(stringToQuote);
            return QUOTE_SYMBOL + stringToQuote + QUOTE_SYMBOL;
        }

        @Override
        protected String doBackward(String s) {
            checkElement(s);
            return unquoteElement(s);
        }

        private static void checkElement(CharSequence element) {
            final boolean isQuoted = isQuotedString(element);
            if (!isQuoted) {
                throw newIllegalArgumentException("Illegal format of the element: " + element);
            }
        }

        private static String unquoteElement(String value) {
            checkNotNull(value);
            final String unquotedValue = Pattern.compile("\\\\")
                                                .matcher(value.substring(2, value.length() - 2))
                                                .replaceAll("");
            return unquotedValue;
        }

        /**
         * Checks that the {@code CharSequence} contains the escaped quotes.
         *
         * @param stringToCheck the sequence of chars to check
         * @return {@code true} if the sequence contains further
         * and prior escaped quotes, {@code false} otherwise
         */
        private static boolean isQuotedString(CharSequence stringToCheck) {
            final int stringLength = stringToCheck.length();

            if (stringLength < 2) {
                return false;
            }

            boolean result = isQuote(stringToCheck.charAt(1)) &&
                             isQuote(stringToCheck.charAt(stringLength - 1));
            return result;
        }

        private static boolean isQuote(char character) {
            return character == QUOTE_SYMBOL;
        }
    }
}

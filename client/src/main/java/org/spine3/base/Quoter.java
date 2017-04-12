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
 * Encloses and discloses the {@code String} objects with double quotes.
 *
 * @author Illia Shepilov
 * @author Alexander Yevsyukov
 */
class Quoter extends Converter<String, String> {
    
    private static final char QUOTE_CHAR = '"';
    private static final String QUOTE = String.valueOf(QUOTE_CHAR);
    private static final String BACKSLASH_QUOTE = "\\\"";
    private static final String DOUBLE_BACKSLASH = "\\\\";
    private static final String ESCAPED_QUOTE = DOUBLE_BACKSLASH + QUOTE_CHAR;
    private static final Pattern DOUBLE_BACKSLASH_PATTERN = Pattern.compile(DOUBLE_BACKSLASH);
    private static final Pattern QUOTE_PATTERN = Pattern.compile(QUOTE);
    private static final String TRIPLE_BACKSLASH = "\\\\\\";
    private static final String DELIMITER_PATTERN_PREFIX = "(?<!" + DOUBLE_BACKSLASH + ')'
                                                            + TRIPLE_BACKSLASH;

    @Override
    protected String doForward(String s) {
        return quote(s);
    }

    @Override
    protected String doBackward(String s) {
        checkNotNull(s);
        return unquote(s);
    }

    /**
     * Prepends quote characters in the passed string with two leading backslashes,
     * and then wraps the string into quotes.
     */
    private static String quote(String stringToQuote) {
        checkNotNull(stringToQuote);
        final String escapedString = QUOTE_PATTERN.matcher(stringToQuote)
                                                  .replaceAll(ESCAPED_QUOTE);
        final String result = QUOTE_CHAR + escapedString + QUOTE_CHAR;
        return result;
    }

    /**
     * Unquotes the passed string and removes double backslash prefixes for quote symbols
     * found inside the passed value.
     */
    private static String unquote(String value) {
        checkQuoted(value);
        final String unquoted = value.substring(2, value.length() - 2);
        final String unescaped = DOUBLE_BACKSLASH_PATTERN.matcher(unquoted)
                                                         .replaceAll("");
        return unescaped;
    }

    /**
     * @throws IllegalArgumentException if the passed char sequence is not wrapped into {@code \"}
     */
    private static void checkQuoted(String str) {
        if (!(str.startsWith(BACKSLASH_QUOTE)
                && str.endsWith(BACKSLASH_QUOTE))) {
            throw newIllegalArgumentException("The passed string is not quoted: %s", str);
        }
    }

    /**
     * Creates the pattern to match the escaped delimiters.
     *
     * @param delimiter the character to match
     * @return the created pattern
     */
    static String createDelimiterPattern(char delimiter) {
        return Pattern.compile(DELIMITER_PATTERN_PREFIX + delimiter)
                      .pattern();
    }

    private enum Singleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Quoter value = new Quoter();
    }

    static Quoter instance() {
        return Singleton.INSTANCE.value;
    }
}

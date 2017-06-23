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

import com.google.common.base.Converter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.util.regex.Pattern.compile;

/**
 * Encloses and discloses the {@code String} objects with double quotes.
 *
 * @author Illia Shepilov
 * @author Alexander Yevsyukov
 */
abstract class Quoter extends Converter<String, String> {

    private static final String BACKSLASH_QUOTE = "\\\"";
    private static final String BACKSLASH = "\\\\";
    private static final char QUOTE_CHAR = '"';
    private static final String DELIMITER_PATTERN_PREFIX = "(?<!" + BACKSLASH + ')' + BACKSLASH;

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
    abstract String quote(String stringToQuote);

    /**
     * Unquotes the passed string and removes double backslash prefixes for quote symbols
     * found inside the passed value.
     */
    abstract String unquote(String value);

    /**
     * Creates the pattern to match the escaped delimiters.
     *
     * @param delimiter the character to match
     * @return the created pattern
     */
    static String createDelimiterPattern(char delimiter) {
        final String quotedDelimiter = Pattern.quote(String.valueOf(delimiter));
        final String result = compile(DELIMITER_PATTERN_PREFIX + quotedDelimiter)
                .pattern();
        return result;
    }

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    private enum Singleton {
        INSTANCE;

        private final Quoter mapQuoter = new MapQuoter();
        private final Quoter listQuoter = new ListQuoter();
    }

    /**
     * Returns the {@code MapQuoter} instance.
     */
    static Quoter forMaps() {
        return Singleton.INSTANCE.mapQuoter;
    }

    /**
     * Returns the {@code ListQuoter} instance.
     */
    static Quoter forLists() {
        return Singleton.INSTANCE.listQuoter;
    }

    /**
     * The {@code Quoter} for the {@code Map}.
     */
    private static class MapQuoter extends Quoter {

        private static final String QUOTE_PATTERN = "((?=[^\\\\])[^\\w])";
        private static final Pattern DOUBLE_BACKSLASH_PATTERN = compile(BACKSLASH);

        @Override
        String quote(String stringToQuote) {
            checkNotNull(stringToQuote);
            final Matcher matcher = compile(QUOTE_PATTERN).matcher(stringToQuote);
            final String unslashed = matcher.find() ?
                                     matcher.replaceAll(BACKSLASH + matcher.group()) :
                                     stringToQuote;
            final String result = QUOTE_CHAR + unslashed + QUOTE_CHAR;
            return result;
        }

        @Override
        String unquote(String value) {
            return unquoteValue(value, DOUBLE_BACKSLASH_PATTERN);
        }
    }

    /**
     * The {@code Quoter} for the {@code List}.
     */
    private static class ListQuoter extends Quoter {

        private static final String BACKSLASH_PATTERN_VALUE = "\\\\\\\\";
        private static final Pattern BACKSLASH_LIST_PATTERN = compile(BACKSLASH_PATTERN_VALUE);
        private static final String ESCAPED_QUOTE = BACKSLASH + QUOTE_CHAR;
        private static final String QUOTE = String.valueOf(QUOTE_CHAR);
        private static final Pattern QUOTE_PATTERN = compile(QUOTE);

        @Override
        String quote(String stringToQuote) {
            checkNotNull(stringToQuote);
            final String escaped = QUOTE_PATTERN.matcher(stringToQuote)
                                                .replaceAll(ESCAPED_QUOTE);
            final String result = QUOTE_CHAR + escaped + QUOTE_CHAR;
            return result;
        }

        @Override
        String unquote(String value) {
            return unquoteValue(value, BACKSLASH_LIST_PATTERN);
        }
    }

    private static String unquoteValue(String value, Pattern pattern) {
        checkQuoted(value);
        final String unquoted = value.substring(2, value.length() - 2);
        final String unescaped = pattern.matcher(unquoted)
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
}

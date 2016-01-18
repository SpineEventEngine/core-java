/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
package org.spine3.io.file;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.escape.CharEscaper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class allows to escape non-letter characters which are not allowed in file names on Windows, Linux and Mac.
 *
 * @author Alexander Yevsyukov
 * @author Alexander Litus
 */
public class FileNameEscaper extends CharEscaper {

    private FileNameEscaper() {}

    public static FileNameEscaper getInstance() {
        return Singleton.INSTANCE.value;
    }

    @Override
    public String escape(@Nonnull String s) {
        return super.escape(s);
    }

    public String decode(String input) {
        return performDecoding(input);
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private static String performDecoding(String value) {
        String interim = value;
        for (BrokenChar brokenChar : BrokenChar.values()) {
            final StringBuffer buffer = new StringBuffer(interim.length());

            final Matcher m = brokenChar.backPattern.matcher(interim);
            while (m.find()) {
                m.appendReplacement(buffer, brokenChar.charBack);
            }
            m.appendTail(buffer);
            interim = buffer.toString();
        }
        return interim;
    }

    @Override
    @Nullable
    protected char[] escape(char c) {
        for (BrokenChar brokenChar : BrokenChar.values()) {
            if (brokenChar.value == c) {
                return brokenChar.replacementArray;
            }
        }
        return null;
    }

    private enum Singleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final FileNameEscaper value = new FileNameEscaper();
    }

    enum BrokenChar {

        BACKWARD_SLASH('\\', "&#92;"),

        FORWARD_SLASH('/', "&#47;"),

        SEMICOLON(':', "&#58;"),

        ASTERISK('*', "&#42;"),

        QUESTION_MARK('?', "&#63;"),

        QUOTES('\"', "&#34;"),

        LT('<', "&#60;"),

        GT('>', "&#62;"),

        VERTICAL_BAR('|', "&#124;"),

        NULL_SIGN('\0', "&#null-sign;");


        @VisibleForTesting
        static final CharMatcher ANY = CharMatcher.anyOf(valuesAsString());

        private final char value;

        private final char[] replacementArray;
        private final String charBack;
        private final Pattern backPattern;

        BrokenChar(char value, String replacement) {
            this.value = value;
            // Cache values
            this.replacementArray = replacement.toCharArray();
            this.charBack = String.valueOf(value);
            this.backPattern = Pattern.compile(replacement);
        }

        static String valuesAsString() {
            final StringBuilder builder = new StringBuilder(values().length);
            for (BrokenChar brokenChar : values()) {
                builder.append(brokenChar.value);
            }
            return builder.toString();
        }
    }
}


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

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.spine3.validate.ConversionError;
import org.spine3.validate.IllegalConversionArgumentException;

import java.text.ParseException;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

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

    protected static class TimestampStringifer extends Stringifier<Timestamp> {

        private static final Pattern PATTERN_COLON = Pattern.compile(":");
        private static final Pattern PATTERN_T = Pattern.compile("T");

        @Override
        protected String doForward(Timestamp timestamp) {
            final String result = toIdString(timestamp);
            return result;
        }

        @Override
        @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
        // It is OK, because all necessary information about exception
        // is passed to the {@code ConversionError} instance.
        protected Timestamp doBackward(String s) {
            try {
                return Timestamps.parse(s);
            } catch (ParseException e) {
                final ConversionError conversionError = new ConversionError(e.getMessage());
                throw new IllegalConversionArgumentException(conversionError);
            }
        }

        /**
         * Converts the passed timestamp to the string.
         *
         * @param timestamp the value to convert
         * @return the string representation of the timestamp
         */
        private static String toIdString(Timestamp timestamp) {
            checkNotNull(timestamp);
            String result = Timestamps.toString(timestamp);
            result = PATTERN_COLON.matcher(result)
                                  .replaceAll("-");
            result = PATTERN_T.matcher(result)
                              .replaceAll("_T");

            return result;
        }
    }

    protected static class EventIdStringifier extends Stringifier<EventId> {
        @Override
        protected String doForward(EventId eventId) {
            final String result = eventId.getUuid();
            return result;
        }

        @Override
        protected EventId doBackward(String s) {
            final EventId result = EventId.newBuilder()
                                          .setUuid(s)
                                          .build();
            return result;
        }
    }

    protected static class CommandIdStringifier extends Stringifier<CommandId> {
        @Override
        protected String doForward(CommandId commandId) {
            final String result = commandId.getUuid();
            return result;
        }

        @Override
        protected CommandId doBackward(String s) {
            final CommandId result = CommandId.newBuilder()
                                              .setUuid(s)
                                              .build();
            return result;
        }
    }
}

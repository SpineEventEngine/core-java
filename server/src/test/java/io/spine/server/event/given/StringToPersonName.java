/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.event.given;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.spine.core.EventContext;
import io.spine.people.PersonName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.BiFunction;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Converts a string into a {@link PersonName} instance.
 *
 * <p>Uses the simplest approach for setting a person name by concatenating
 * strings starting from the second.
 *
 * <p>Production code should analyze the content of the names and set
 * corresponding fields of the {@code PersonName}.
 *
 * @author Alexander Yevsyukov
 */
public class StringToPersonName implements BiFunction<String, EventContext, PersonName> {

    private static final Splitter SPLITTER = Splitter.on(' ');
    private static final Joiner JOINER = Joiner.on(' ');

    @Override
    public @Nullable PersonName apply(@Nullable String input, EventContext context) {
        if (input == null) {
            return PersonName.getDefaultInstance();
        }
        List<String> names = newArrayList(SPLITTER.split(input));
        PersonName.Builder builder = PersonName
                .newBuilder()
                .setGivenName(names.get(0));
        addOtherNames(builder, names);
        return builder.build();
    }

    @SuppressWarnings("CheckReturnValue") // calling builders
    private static void addOtherNames(PersonName.Builder builder, List<String> names) {
        if (names.size() > 1) {
            List<String> otherNames = names.subList(1, names.size() - 1);
            String joined = JOINER.join(otherNames);
            builder.setFamilyName(joined);
        }
    }
}

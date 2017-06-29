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

package io.spine.core;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.spine.client.TestActorRequestFactory;
import io.spine.time.Time;
import org.junit.Before;
import org.junit.Test;

import static io.spine.protobuf.TypeConverter.toMessage;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class CommandAttributeShould {

    private final TestActorRequestFactory factory =
            TestActorRequestFactory.newInstance(CommandAttributeShould.class);

    private CommandContext.Builder contextBuilder;

    @Before
    public void setUp() {
        Command command = factory.createCommand(Empty.getDefaultInstance(),
                                                Time.getCurrentTime());
        contextBuilder = command.getContext()
                                .toBuilder();
    }

    private <T> void assertSetGet(CommandAttribute<T> attr, T value) {
        attr.setValue(contextBuilder, value);

        assertEquals(value, attr.getValue(contextBuilder.build())
                                .get());
    }

    @Test
    public void set_and_get_bool_attribute() {
        final CommandAttribute<Boolean> attr = new CommandAttribute<Boolean>("flag") {
        };
        assertSetGet(attr, true);
        assertSetGet(attr, false);
    }

    @Test
    public void set_and_get_string_attribute() {
        final CommandAttribute<String> attr = new CommandAttribute<String>("str") {
        };
        final String value = getClass().getName();

        assertSetGet(attr, value);
    }

    @Test
    public void set_and_get_long_attribute() {
        final CommandAttribute<Long> attr = new CommandAttribute<Long>("l-o-n-g") {
        };
        final Long value = 10101010L;

        assertSetGet(attr, value);
    }

    @Test
    public void set_and_get_int_attribute() {
        final CommandAttribute<Integer> attr = new CommandAttribute<Integer>("int") {
        };
        final Integer value = 1024;

        assertSetGet(attr, value);
    }

    @Test
    public void set_and_get_message_attribute() {
        final CommandAttribute<StringValue> attr = new CommandAttribute<StringValue>("str-val") {
        };
        final StringValue value = toMessage(getClass().getName());

        assertSetGet(attr, value);
    }

    @Test
    public void set_and_get_float_attribute() {
        final CommandAttribute<Float> attr = new CommandAttribute<Float>("flp") {
        };
        final Float value = 1024.512f;

        assertSetGet(attr, value);
    }

    @Test
    public void set_and_get_double_attribute() {
        final CommandAttribute<Double> attr = new CommandAttribute<Double>("dbl") {
        };
        final Double value = 100.500;

        assertSetGet(attr, value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_on_unsupported_type() {
        final CommandAttribute<Object> attr = new CommandAttribute<Object>("o") {
        };

        @SuppressWarnings("EmptyClass") final Object value = new Object() {
        };

        attr.setValue(contextBuilder, value);
    }
}

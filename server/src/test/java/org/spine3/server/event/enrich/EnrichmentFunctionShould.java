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

package org.spine3.server.event.enrich;

import com.google.common.base.Function;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.protobuf.Values;
import org.spine3.test.Tests;

import javax.annotation.Nullable;

import static org.junit.Assert.*;
import static org.spine3.server.event.enrich.EventMessageEnricher.unboundInstance;
import static org.spine3.server.event.enrich.FieldEnricher.newInstance;

public class EnrichmentFunctionShould {

    private Function<Int32Value, StringValue> function;

    @Before
    public void setUp() {
        function = new Function<Int32Value, StringValue>() {
            @Nullable
            @Override
            public StringValue apply(@Nullable Int32Value input) {
                if (input == null) {
                    return null;
                }
                final String inputStr = String.valueOf(input.getValue());
                return Values.newStringValue(inputStr + '+' + inputStr);
            }
        };
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_source_class() {
        unboundInstance(Tests.<Class<? extends Message>>nullRef(), StringValue.class);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_target_class() {
        unboundInstance(StringValue.class, Tests.<Class<? extends Message>>nullRef());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_accept_same_source_and_target_class() {
        unboundInstance(StringValue.class, StringValue.class);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_translator() {
        newInstance(BoolValue.class, StringValue.class, Tests.<Function<BoolValue, StringValue>>nullRef());
    }

    @Test
    public void return_sourceClass() throws Exception {
        assertEquals(Int32Value.class, unboundInstance(Int32Value.class, Int64Value.class).getSourceClass());
    }

    @Test
    public void return_targetClass() throws Exception {
        assertEquals(Int64Value.class, unboundInstance(Int32Value.class, Int64Value.class).getTargetClass());
    }

    @Test
    public void create_custom_instances() throws Exception {
        final EnrichmentFunction<Int32Value, StringValue> c1 = newInstance(Int32Value.class,
                                                                           StringValue.class,
                                                                           function);
        final EnrichmentFunction<Int32Value, StringValue> c2 = newInstance(Int32Value.class,
                                                                           StringValue.class,
                                                                           function);
        assertEquals(c1, c2);
    }

    @Test
    public void apply_enrichment() throws Exception {

        final EnrichmentFunction<Int32Value, StringValue> func = newInstance(Int32Value.class,
                                                                             StringValue.class,
                                                                             function);

        final StringValue encriched = func.apply(Values.newIntegerValue(2));
        assertNotNull(encriched);
        assertEquals("2+2", encriched.getValue());
    }

    @Test
    public void have_hashCode() throws Exception {
        final EnrichmentFunction<BoolValue, StringValue> function = unboundInstance(BoolValue.class, StringValue.class);
        assertTrue(function.hashCode() != System.identityHashCode(function));
    }

    @Test
    public void have_toString() throws Exception {
        final EnrichmentFunction<Int32Value, StringValue> func = newInstance(Int32Value.class,
                                                                             StringValue.class,
                                                                             function);
        final String out = func.toString();
        assertTrue(out.contains(Int32Value.class.getName()));
        assertTrue(out.contains(StringValue.class.getName()));
        assertTrue(out.contains(func.getFunction().toString()));
    }

    @Test
    public void return_null_on_applying_null() {
        final FieldEnricher<Int32Value, StringValue> fieldEnricher = newInstance(Int32Value.class,
                                                                                 StringValue.class,
                                                                                 function);
        assertNull(fieldEnricher.apply(Tests.<Int32Value>nullRef()));
    }

    @Test
    public void have_smart_equals() {
        final EnrichmentFunction<Int32Value, StringValue> func = newInstance(Int32Value.class,
                                                                            StringValue.class,
                                                                            function);
        //noinspection EqualsWithItself
        assertTrue(func.equals(func));
        assertFalse(func.equals(Tests.<EnrichmentFunction<Int32Value, StringValue>>nullRef()));
    }
}
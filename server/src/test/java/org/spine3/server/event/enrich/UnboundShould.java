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

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class UnboundShould {

    private final Unbound<BoolValue, StringValue> unbound = Unbound.newInstance(BoolValue.class, StringValue.class);

    @Test
    public void return_null_on_getFunction() throws Exception {
        assertNull(unbound.getFunction());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void throw_unsupported_on_apply() {
        unbound.apply(BoolValue.getDefaultInstance());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void throw_exception_on_validate() {
        unbound.validate();
    }

    @Test
    public void convert_to_bound() {
        final EventMessageEnricher<BoolValue, StringValue> enricher = unbound.toBound(EventEnricher.newBuilder().build());
        assertNotNull(enricher);
    }
}

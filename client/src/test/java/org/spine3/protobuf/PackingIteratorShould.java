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

package org.spine3.protobuf;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.protobuf.Wrapper.forInteger;
import static org.spine3.protobuf.Wrapper.forLong;
import static org.spine3.protobuf.Wrapper.forUnsignedInteger;
import static org.spine3.protobuf.Wrapper.forUnsignedLong;
import static org.spine3.validate.Validate.isDefault;

/**
 * @author Alexander Yevsyukov
 */
public class PackingIteratorShould {

    private List<Message> list;
    private Iterator<Any> packer;

    @Before
    public void setUp() {
        list = Lists.<Message>newArrayList(
                Wrapper.forString("one"),
                forInteger(2),
                forLong(3),
                forUnsignedInteger(4),
                forUnsignedLong(5));
        packer = new PackingIterator(list.iterator());
    }

    @Test
    public void implement_hasNext() throws Exception {
        assertTrue(packer.hasNext());

        list.clear();

        assertFalse(packer.hasNext());
    }

    @Test
    public void implement_next() throws Exception {
        while (packer.hasNext()) {
            final Any packed = packer.next();
            assertNotNull(packed);
            assertFalse(isDefault(unpack(packed)));
        }
    }
}

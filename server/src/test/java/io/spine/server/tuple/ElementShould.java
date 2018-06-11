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

package io.spine.server.tuple;

import com.google.common.base.Optional;
import com.google.common.testing.EqualsTester;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.test.TestValues;
import org.junit.Test;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;

/**
 * @author Alexander Yevsyukov
 */
public class ElementShould {

    @Test
    public void support_equality() {
        Timestamp time = Time.getCurrentTime();
        new EqualsTester().addEqualityGroup(new Element(time), new Element(time))
                          .addEqualityGroup(new Element(TestValues.newUuidValue()))
                          .addEqualityGroup(new Element(Optional.absent()))
                          .testEquals();
    }

    @Test
    public void serialize() {
        reserializeAndAssert(new Element(Time.getCurrentTime()));
        reserializeAndAssert(new Element(Optional.of(Time.getCurrentTime())));
        reserializeAndAssert(new Element(Optional.absent()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void restrict_possible_value_types() {
        new Element(getClass());
    }
}

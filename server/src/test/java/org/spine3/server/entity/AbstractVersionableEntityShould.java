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

package org.spine3.server.entity;

import com.google.common.testing.EqualsTester;
import com.google.protobuf.StringValue;
import org.junit.Test;

/**
 * @author Alexander Yevsyukov
 */
public class AbstractVersionableEntityShould {

    @SuppressWarnings("MagicNumber")
    @Test
    public void have_equals() throws Exception {
        final AvEntity entity = new AvEntity(88L);
        final AvEntity another = new AvEntity(88L);
        another.setState(entity.getState(), entity.getVersion()
                                                  .getNumber(), entity.getVersion()
                                                                      .getTimestamp());

        new EqualsTester().addEqualityGroup(entity, another)
                          .addEqualityGroup(new AvEntity(42L))
                          .testEquals();
    }

    @Test
    public void have_hashCode() throws Exception {

    }

    private static class AvEntity extends AbstractVersionableEntity<Long, StringValue> {
        protected AvEntity(Long id) {
            super(id);
        }
    }
}

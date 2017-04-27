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

import com.google.protobuf.Empty;
import org.junit.Test;
import org.spine3.test.TestActorRequestFactory;
import org.spine3.time.Time;

import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class CommandAttributeShould {

    private final TestActorRequestFactory factory =
            TestActorRequestFactory.newInstance(CommandAttributeShould.class);

    private final CommandAttribute<Boolean> boolAttr = new CommandAttribute<Boolean>(
            Attribute.Type.BOOLEAN, "flag") {
    };

    @Test
    public void set_and_get_bool_attribute() {
        final Command command = factory.createCommand(Empty.getDefaultInstance(),
                                                      Time.getCurrentTime());
        final CommandContext.Builder builder = command.getContext()
                                                      .toBuilder();
        boolAttr.set(builder, true);

        assertTrue(boolAttr.get(builder.build())
                           .get());
    }

}

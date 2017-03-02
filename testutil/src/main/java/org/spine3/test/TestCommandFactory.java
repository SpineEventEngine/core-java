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

package org.spine3.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.Internal;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.client.CommandFactory;
import org.spine3.time.ZoneOffset;
import org.spine3.time.ZoneOffsets;
import org.spine3.users.UserId;

import static org.spine3.test.Tests.newUserId;

/**
 * The command factory, which allows generating commands as if the were
 * created at the specified moments in time.
 *
 * @author Alexaner Yevsyukov
 */
@Internal
@VisibleForTesting
public class TestCommandFactory extends CommandFactory {

    protected TestCommandFactory(UserId actor, ZoneOffset zoneOffset) {
        super(newBuilder().setActor(actor)
                          .setZoneOffset(zoneOffset));
    }

    public static TestCommandFactory newInstance(String actor, ZoneOffset zoneOffset) {
        return new TestCommandFactory(newUserId(actor), zoneOffset);
    }

    public static TestCommandFactory newInstance(Class<?> testClass) {
        return newInstance(testClass.getName(), ZoneOffsets.UTC);
    }

    /** Creates new command with the passed timestamp. */
    public Command createCommand(Message message, Timestamp timestamp) {
        final Command command = createCommand(message);
        return TimeTests.adjustTimestamp(command, timestamp);

    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open access to creating command contexts in tests.
     */
    @Override
    public CommandContext createContext() {
        return super.createContext();
    }
}

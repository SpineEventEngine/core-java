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

package io.spine.server.rejection.given;

import com.google.protobuf.Any;
import io.spine.core.Event;
import io.spine.core.Subscribe;
import io.spine.test.rejection.ProjectRejections;
import io.spine.test.rejection.command.RjRemoveOwner;

import static io.spine.core.Events.getMessage;
import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class CommandMessageAwareSubscriber extends VerifiableSubscriber {

    private ProjectRejections.MissingOwner rejection;
    private RjRemoveOwner command;

    @Subscribe
    public void on(ProjectRejections.MissingOwner rejection, RjRemoveOwner command) {
        triggerCall();
        this.rejection = rejection;
        this.command = command;
    }

    @Override
    public void verifyGot(Event event) {
        assertEquals(getMessage(event), rejection);
        Any commandMessage = event.getContext()
                                  .getRejection()
                                  .getCommandMessage();
        assertEquals(unpack(commandMessage), command);
    }
}

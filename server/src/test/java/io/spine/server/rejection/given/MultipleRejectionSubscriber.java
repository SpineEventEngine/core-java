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

import com.google.protobuf.Message;
import io.spine.core.Subscribe;
import io.spine.server.entity.rejection.StandardRejections.CannotModifyDeletedEntity;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.test.rejection.command.RjRemoveOwner;
import io.spine.test.rejection.command.RjStartProject;
import io.spine.test.rejection.command.RjUpdateProjectName;

/**
 * A rejection subscriber to test filtering of rejections using command message parameter.
 *
 * @author Dmytro Grankin
 */
public class MultipleRejectionSubscriber extends RejectionSubscriber {

    private int subscriberCalls = 0;
    private Class<? extends Message> commandMessageClass;

    @Subscribe
    public void on(CannotModifyDeletedEntity rejection,
                   RjStartProject command) {
        handleRejectionWithCommandMessage(command);
    }

    @Subscribe
    public void on(CannotModifyDeletedEntity rejection,
                   RjUpdateProjectName command) {
        handleRejectionWithCommandMessage(command);
    }

    @Subscribe
    public void on(CannotModifyDeletedEntity rejection,
                   RjRemoveOwner command) {
        handleRejectionWithCommandMessage(command);
    }

    @Subscribe
    public void on(CannotModifyDeletedEntity rejection) {
        subscriberCalls++;
    }

    private void handleRejectionWithCommandMessage(Message commandMessage) {
        subscriberCalls++;
        commandMessageClass = commandMessage.getClass();
    }

    public int numberOfSubscriberCalls() {
        return subscriberCalls;
    }

    public Class<? extends Message> commandMessageClass() {
        return commandMessageClass;
    }
}

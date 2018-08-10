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

import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.server.rejection.RejectionDispatcher;
import io.spine.test.rejection.ProjectRejections.InvalidProjectName;

import java.util.Set;

/**
 * A simple dispatcher class, which only dispatch and does not have own rejection subscribing
 * methods.
 *
 * @author Alexander Yevsyukov
 */
public class BareDispatcher implements RejectionDispatcher<String> {

    private boolean dispatchCalled = false;

    @Override
    public Set<RejectionClass> getMessageClasses() {
        return RejectionClass.setOf(InvalidProjectName.class);
    }

    @Override
    public Set<String> dispatch(RejectionEnvelope rejection) {
        dispatchCalled = true;
        return identity();
    }

    @Override
    public void onError(RejectionEnvelope envelope, RuntimeException exception) {
        // Do nothing.
    }

    public boolean isDispatchCalled() {
        return dispatchCalled;
    }
}

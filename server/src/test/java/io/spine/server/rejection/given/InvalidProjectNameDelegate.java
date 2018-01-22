/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.server.rejection.RejectionDispatcherDelegate;
import io.spine.test.rejection.ProjectRejections.InvalidProjectName;

import java.util.Set;

/**
 * A test delegate, which dispatches {@link InvalidProjectName} rejections.
 *
 * @author Alex Tymchenko
 */
public class InvalidProjectNameDelegate implements RejectionDispatcherDelegate<String> {

    private boolean dispatchCalled = false;

    @Override
    public Set<RejectionClass> getRejectionClasses() {
        return RejectionClass.setOf(InvalidProjectName.class);
    }

    @Override
    public Set<RejectionClass> getExternalRejectionClasses() {
        return ImmutableSet.of();
    }

    @Override
    public Set<String> dispatchRejection(RejectionEnvelope envelope) {
        dispatchCalled = true;
        return ImmutableSet.of(toString());
    }

    @Override
    public void onError(RejectionEnvelope envelope, RuntimeException exception) {
        // do nothing.
    }

    public boolean isDispatchCalled() {
        return dispatchCalled;
    }
}

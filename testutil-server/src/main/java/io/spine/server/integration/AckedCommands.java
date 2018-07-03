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

package io.spine.server.integration;

import com.google.common.annotations.VisibleForTesting;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Rejection;
import io.spine.core.Status;
import io.spine.grpc.MemoizingObserver;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@VisibleForTesting
class AckedCommands {

    private static final Rejection EMPTY_REJECTION = Rejection.getDefaultInstance();
    private static final Error EMPTY_ERROR = Error.getDefaultInstance();
    
    private final List<Ack> acks = newArrayList();
    private final List<Error> errors = newArrayList();
    private final List<Rejection> rejections = newArrayList();

    AckedCommands(MemoizingObserver<Ack> observer) {
        List<Ack> responses = observer.responses();
        for (Ack response : responses) {
            acks.add(response);

            Status status = response.getStatus();
         
            Error error = status.getError();
            if (!error.equals(EMPTY_ERROR)) {
                errors.add(error);
            }

            Rejection rejection = status.getRejection();
            if (!rejection.equals(EMPTY_REJECTION)) {
                rejections.add(rejection);
            }
        }
    }

    boolean withoutErrors() {
        return errors.isEmpty();
    }

    boolean withoutRejections() {
        return rejections.isEmpty();
    }

    public int count() {
        return acks.size();
    }
}

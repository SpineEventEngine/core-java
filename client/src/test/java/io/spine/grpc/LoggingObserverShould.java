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

package io.spine.grpc;

import io.spine.grpc.LoggingObserver.Level;
import io.spine.time.Time;
import org.junit.Test;

import static io.spine.Identifier.newUuid;
import static org.junit.Assert.assertNotNull;

/**
 * @author Alexander Yevsyukov
 */
public class LoggingObserverShould {

    @Test
    public void have_several_levels() {
        assertAtLevel(Level.TRACE);
        assertAtLevel(Level.DEBUG);
        assertAtLevel(Level.INFO);
        assertAtLevel(Level.WARN);
    }

    private void assertAtLevel(Level level) {
        final LoggingObserver<Object> observer = LoggingObserver.forClass(getClass(), level);

        assertNotNull(observer);

        observer.onNext(newUuid());
        observer.onNext(Time.getCurrentTime());
        observer.onCompleted();
    }

    @Test
    public void log_error() {
        final LoggingObserver<Object> observer = LoggingObserver.forClass(getClass(), Level.INFO);
        observer.onError(new RuntimeException("Testing logging observer"));
    }
}

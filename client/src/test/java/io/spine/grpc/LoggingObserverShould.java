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

import com.google.common.collect.Queues;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.grpc.LoggingObserver.Level;
import io.spine.time.Time;
import org.junit.Test;
import org.slf4j.event.EventRecodingLogger;
import org.slf4j.event.LoggingEvent;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.SubstituteLogger;

import java.util.Queue;

import static io.spine.Identifier.newUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class LoggingObserverShould {

    @Test
    public void have_TRACE_level() {
        assertAtLevel(Level.TRACE);
    }

    @Test
    public void have_DEBUG_levels() {
        assertAtLevel(Level.DEBUG);
    }

    @Test
    public void have_INFO_level() {
        assertAtLevel(Level.INFO);
    }

    @Test
    public void have_WARN_level() {
        assertAtLevel(Level.WARN);
    }

    private void assertAtLevel(Level level) {
        final LoggingObserver<Object> observer = getObserver(level);

        assertNotNull(observer);

        // Since we're in the tests mode `Environment` returns `SubstituteLogger` instance.
        final SubstituteLogger log = (SubstituteLogger) observer.log();

        // Restrict the queue size only to the number of calls we want to make.
        final Queue<SubstituteLoggingEvent> queue = Queues.newArrayBlockingQueue(3);
        log.setDelegate(new EventRecodingLogger(log, queue));

        SubstituteLoggingEvent loggingEvent;

        final String value = newUuid();
        observer.onNext(value);
        loggingEvent = queue.poll();
        assertNotNull(loggingEvent);
        assertLogLevel(loggingEvent, level);
        assertContains(loggingEvent, value);
        assertContains(loggingEvent, "onNext");

        final Timestamp currentTime = Time.getCurrentTime();
        final String timeStr = Timestamps.toString(currentTime);
        observer.onNext(currentTime);
        loggingEvent = queue.poll();
        assertNotNull(loggingEvent);
        assertLogLevel(loggingEvent, level);
        assertContains(loggingEvent, timeStr);

        observer.onCompleted();
        loggingEvent = queue.poll();
        assertNotNull(loggingEvent);
        assertLogLevel(loggingEvent, level);
        assertContains(loggingEvent, "onCompleted");
    }

    private static void assertLogLevel(LoggingEvent event, Level level) {
        switch (level) {
            case TRACE:
                assertEquals(org.slf4j.event.Level.TRACE, event.getLevel());
                break;
            case DEBUG:
                assertEquals(org.slf4j.event.Level.DEBUG, event.getLevel());
                break;
            case INFO:
                assertEquals(org.slf4j.event.Level.INFO, event.getLevel());
                break;
            case WARN:
                assertEquals(org.slf4j.event.Level.WARN, event.getLevel());
                break;
        }
    }

    private static void assertContains(LoggingEvent event, String text) {
        final String message = event.getMessage();
        assertTrue(message.contains(text));
    }

    @Test
    public void log_error() {
        final LoggingObserver<Object> observer = getObserver(Level.INFO);
        observer.onError(new RuntimeException("Testing logging observer"));
    }

    private LoggingObserver<Object> getObserver(Level level) {
        return LoggingObserver.forClass(getClass(), level);
    }
}

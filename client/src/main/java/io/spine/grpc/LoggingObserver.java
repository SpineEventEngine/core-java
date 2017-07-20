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

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Internal;
import io.grpc.stub.StreamObserver;
import io.spine.Environment;
import io.spine.string.Stringifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static java.lang.String.format;

/**
 * The {@code StreamObserver} which adds records to the log as its methods called.
 *
 * <p>The observer gets a reference to the parent class for which
 * to {@linkplain LoggerFactory#getLogger(Class) create a logger}.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public final class LoggingObserver<V> implements StreamObserver<V> {

    private static final String FMT_ON_NEXT = "onNext(%s)";
    private static final String ON_COMPLETED = "onCompleted()";
    private static final Object[] emptyParam = {};

    private final Class<?> parentClass;
    private final Level level;

    /** The lazily initialized logger. */
    @Nullable
    private volatile Logger logger;

    private LoggingObserver(Class<?> parentClass, Level level) {
        this.parentClass = parentClass;
        this.level = level;
    }

    /**
     * Creates a new instance with logs into the log of the passed class.
     *
     * @param <T>         the type of streamed objects
     * @param parentClass the for which which to {@linkplain LoggerFactory#getLogger(Class)
     *                    create a logger}
     * @param level       the level of logging to use for non-error callbacks
     * @return new instance
     */
    public static <T> LoggingObserver<T> forClass(Class<?> parentClass, Level level) {
        return new LoggingObserver<>(parentClass, level);
    }

    @Override
    public void onNext(V v) {
        logNext(v);
    }

    private void logNext(V v) {
        doLog(FMT_ON_NEXT, v);
    }

    @Override
    public void onError(Throwable throwable) {
        log().error("", throwable);
    }

    @Override
    public void onCompleted() {
        doLog(ON_COMPLETED, null);
    }

    private void doLog(String format, @Nullable V v) {
        final String out;
        if (v != null) {
            final String value = Stringifiers.toString(v);
            out = format(format, value);
        } else {
            out = format;
        }
        final Logger logger = log();
        switch (level) {
            case TRACE:
                logger.trace(out);
                break;
            case DEBUG:
                // We use this call until the following Slf4J issue is fixed.
                //      https://jira.qos.ch/browse/SLF4J-376
                // The bug cases the event created on debug(String) have `TRACE` level.
                logger.debug(out, emptyParam);
                break;
            case INFO:
                logger.info(out);
                break;
            case WARN:
                logger.warn(out);
        }
    }

    /**
     * Obtains logger instance. Lazily creates logger if it's not created yet.
     */
    @SuppressWarnings("SynchronizeOnThis") // See “Effective Java 2nd Edition”, Item #71.
    @VisibleForTesting
    Logger log() {
        Logger result = logger;
        if (result == null) {
            synchronized (this) {
                result = logger;
                if (result == null) {
                    logger = Environment.getInstance().getLogger(parentClass);
                    result = logger;
                }
            }
        }
        return result;
    }

    /**
     * The level of logging to be used for {@link #onNext(Object)} and {@link #onCompleted()}.
     *
     * <p>Errors are reported via {@linkplain Logger#error(String, Throwable)}.
     */
    public enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN
    }
}

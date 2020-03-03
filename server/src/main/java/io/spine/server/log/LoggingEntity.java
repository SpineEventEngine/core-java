/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.log;

import com.google.common.flogger.FluentLogger;
import io.spine.logging.Logging;

import java.util.logging.Level;

/**
 * A {@link Logging} trait for entities.
 *
 * <p>When adding logs in an entity, use this interface over {@link Logging}.
 */
public interface LoggingEntity extends Logging {

    /**
     * Creates a {@code FluentLogger.Api} with the given level.
     *
     * @param level
     *         the log level
     * @return new fluent logging API
     * @apiNote This method mirrors the declaration of
     *         {@link io.spine.server.entity.AbstractEntity#at(Level)}. When a concrete entity
     *         implements this interface, the underscore logging methods will have the same
     *         behaviour regarding the log site as does the {@code AbstractEntity.at(Level)} method.
     */
    FluentLogger.Api at(Level level);

    @Override
    default FluentLogger.Api _severe() {
        return at(Level.SEVERE);
    }

    @Override
    default FluentLogger.Api _warn() {
        return at(Level.WARNING);
    }

    @Override
    default FluentLogger.Api _info() {
        return at(Level.INFO);
    }

    @Override
    default FluentLogger.Api _config() {
        return at(Level.CONFIG);
    }

    @Override
    default FluentLogger.Api _fine() {
        return at(Level.FINE);
    }

    @Override
    default FluentLogger.Api _finer() {
        return at(Level.FINER);
    }

    @Override
    default FluentLogger.Api _finest() {
        return at(Level.FINEST);
    }
}

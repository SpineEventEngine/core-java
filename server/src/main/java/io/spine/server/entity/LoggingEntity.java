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

package io.spine.server.entity;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.LogSite;
import io.spine.logging.Logging;
import io.spine.server.model.HandlerMethod;

import java.util.Optional;

public interface LoggingEntity extends Logging {

    Optional<LogSite> handlerSite();

    void enter(HandlerMethod<?, ?, ?, ?> method);

    void resetLog();

    @Override
    default FluentLogger.Api _severe() {
        FluentLogger.Api log = Logging.super._severe();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }

    @Override
    default FluentLogger.Api _error() {
        FluentLogger.Api log = Logging.super._error();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }

    @Override
    default FluentLogger.Api _warn() {
        FluentLogger.Api log = Logging.super._warn();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }

    @Override
    default FluentLogger.Api _info() {
        FluentLogger.Api log = Logging.super._info();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }

    @Override
    default FluentLogger.Api _config() {
        FluentLogger.Api log = Logging.super._config();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }

    @Override
    default FluentLogger.Api _fine() {
        FluentLogger.Api log = Logging.super._fine();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }

    @Override
    default FluentLogger.Api _debug() {
        FluentLogger.Api log = Logging.super._debug();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }

    @Override
    default FluentLogger.Api _finer() {
        FluentLogger.Api log = Logging.super._finer();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }

    @Override
    default FluentLogger.Api _finest() {
        FluentLogger.Api log = Logging.super._finest();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }

    @Override
    default FluentLogger.Api _trace() {
        FluentLogger.Api log = Logging.super._trace();
        return handlerSite()
                .map(log::withInjectedLogSite)
                .orElse(log);
    }
}

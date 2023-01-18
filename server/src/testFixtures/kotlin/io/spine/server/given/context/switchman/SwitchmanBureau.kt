/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.route.given.switchman;

import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.given.switchman.command.SetSwitch;
import io.spine.server.route.given.switchman.rejection.SwitchmanUnavailable;

/**
 * A repository which fires a rejection in response to a command with a particular value of the
 * target aggregate ID.
 */
public final class SwitchmanBureau extends AggregateRepository<String, Switchman, SwitchmanLog> {

    /** The ID of the aggregate for which a {@link SetSwitch command} would be rejected. */
    public static final String MISSING_SWITCHMAN_NAME = "Petrovich";

    @Override
    protected void setupCommandRouting(CommandRouting<String> routing) {
        super.setupCommandRouting(routing);
        routing.route(SetSwitch.class, (cmd, ctx) -> routeToSwitchman(cmd));
    }

    /**
     * Returns the route to a switchman, checking whether he is available.
     *
     * <p>In case the switchman isn't available, throws a {@code RuntimeException}.
     */
    private static String routeToSwitchman(SetSwitch cmd) {
        var switchmanName = cmd.getSwitchmanName();
        if (switchmanName.equals(MISSING_SWITCHMAN_NAME)) {
            throw new RuntimeException(
                    SwitchmanUnavailable.newBuilder()
                            .setSwitchmanName(switchmanName)
                            .build());
        }
        return switchmanName;
    }
}

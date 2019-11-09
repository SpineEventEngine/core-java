/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.test.client;

import io.spine.core.UserId;
import io.spine.server.command.Assign;
import io.spine.server.procman.ProcessManager;
import io.spine.server.tuple.Pair;
import io.spine.test.client.command.LogInUser;
import io.spine.test.client.command.LogOutUser;
import io.spine.test.client.event.UserAccountCreated;
import io.spine.test.client.event.UserLoggedIn;
import io.spine.test.client.event.UserLoggedOut;
import io.spine.test.client.rejection.UserAlreadyLoggedIn;

/**
 * Performs login/logout operations and keeps the login status of the user.
 */
final class LoginProcess extends ProcessManager<UserId, LoginStatus, LoginStatus.Builder>  {

    @Assign
    Pair<UserLoggedIn, UserAccountCreated> on(LogInUser c) throws UserAlreadyLoggedIn {

        UserId user = c.getUser();
        LoginStatus state = state();
        if (state.getUser().equals(user) && state.getLoggedIn()) {
            throw UserAlreadyLoggedIn
                    .newBuilder()
                    .setUser(user)
                    .build();
        }
        builder()
                .setUser(user)
                .setLoggedIn(true);
        return Pair.of(
                UserLoggedIn
                        .newBuilder()
                        .setUser(user)
                        .vBuild(),
                UserAccountCreated
                        .newBuilder()
                        .setUser(user)
                        .vBuild()
        );
    }

    @Assign
    UserLoggedOut on(LogOutUser c) {
        builder().setLoggedIn(false);
        return UserLoggedOut.newBuilder()
                            .setUser(c.getUser())
                            .vBuild();
    }
}

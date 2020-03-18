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

package io.spine.testing.server.blackbox;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import io.spine.core.TenantId;
import io.spine.server.BoundedContextBuilder;
import io.spine.testing.server.blackbox.command.BbCreateProject;
import io.spine.testing.server.blackbox.event.BbProjectCreated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.testing.core.given.GivenTenantId.generate;
import static io.spine.testing.server.blackbox.given.Given.createProject;
import static io.spine.testing.server.blackbox.given.Given.createdProjectState;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Multi-tenant `BlackBoxContext` should")
class MultiTenantContextTest
        extends BlackBoxContextTest<MultiTenantContext> {

    @Override
    BoundedContextBuilder newBuilder() {
        return BoundedContextBuilder.assumingTests(true);
    }

    @BeforeEach
    @OverridingMethodsMustInvokeSuper
    @Override
    void setUp() {
        super.setUp();
        context().withTenant(generate());
    }

    @Test
    @DisplayName("verify using a particular tenant ID")
    void verifyForDifferentTenants() {
        TenantId john = generate();
        TenantId carl = generate();
        TenantId newUser = generate();
        BbCreateProject createJohnProject = createProject();
        BbCreateProject createCarlProject = createProject();
        MultiTenantContext context = context()
                // Create a project for John.
                .withTenant(john)
                .receivesCommand(createJohnProject)

                // Create a project for Carl.
                .withTenant(carl)
                .receivesCommand(createCarlProject);
        // Verify project was created for John.
        context.withTenant(john)
               .assertEvents()
               .withType(BbProjectCreated.class)
               .hasSize(1);
        context.assertEntityWithState(BbProject.class, createJohnProject.getProjectId())
               .hasStateThat()
               .isEqualTo(createdProjectState(createJohnProject));
        // Verify project was created for Carl.
        MultiTenantContext contextForCarl = context.withTenant(carl);
        contextForCarl
                .assertEvents()
                .withType(BbProjectCreated.class)
                .hasSize(1);
        contextForCarl
                .assertEntityWithState(BbProject.class, createCarlProject.getProjectId())
                .hasStateThat()
                .isEqualTo(createdProjectState(createCarlProject));
        // Verify nothing happened for a new user.
        MultiTenantContext newUserContext = context.withTenant(newUser);
        newUserContext
                .assertCommands()
                .isEmpty();
        newUserContext
                .assertEvents()
                .isEmpty();
    }

    @Test
    @DisplayName("require tenant ID")
    void requireTenantId() {
        assertThrows(
                IllegalStateException.class,
                () -> BlackBoxContext.from(BoundedContextBuilder.assumingTests(true))
                                     .assertEntityWithState(BbProject.class, "verify state")
        );
    }
}

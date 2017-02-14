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

package org.spine3.server.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.spine3.base.Command;
import org.spine3.base.CommandId;
import org.spine3.users.TenantId;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.validate.Validate.isDefault;

/**
 * An abstract base for operations on a tenant data.
 *
 * @author Alexander Yevsyukov
 * @see #execute()
 */
public abstract class TenantDataOperation implements Runnable {

    private final TenantId tenantId;

    /**
     * Contains the ID of the currently being handled command,
     * or {@code null} if the operation is performed not because of a command.
     */
    @Nullable
    private final CommandId commandId;

    /**
     * Creates a new instance.
     *
     * <p>If default value of tenant ID is passed, {@link CurrentTenant#singleTenant()}
     * will be substituted.
     *
     * @param tenantId tenant ID or default value
     * @param commandId a command ID or {@code null}
     */
    private TenantDataOperation(TenantId tenantId, @Nullable CommandId commandId) {
        checkNotNull(tenantId);
        this.tenantId = isDefault(tenantId)
                        ? CurrentTenant.singleTenant()
                        : tenantId;
        this.commandId = commandId;
    }

    /**
     * Creates an instance of the operation, which uses the {@code TenantId}
     * set in the current non-command handling execution context.
     *
     * @throws IllegalStateException if there is no current {@code TenantId}
     * @see CurrentTenant#ensure()
     */
    protected TenantDataOperation() throws IllegalStateException {
        this(CurrentTenant.ensure(), null);
    }

    /**
     * Creates an instance for the operation for the tenant specified
     * by the passed ID.
     *
     * <p>This constructor must be called for non-command handling execution context.
     *
     * <p>If default instance of {@link TenantId} is passed (because
     * the application works in a single-tenant mode, the value
     * returned by {@link CurrentTenant#singleTenant()} will be substituted.
     *
     * @param tenantId the tenant ID or {@linkplain TenantId#getDefaultInstance() default value}
     */
    protected TenantDataOperation(TenantId tenantId) {
        this(tenantId, null);
    }

    /**
     * Creates and instance for the operation on the tenant data in
     * response to the passed command.
     *
     * @param command the command from which context to obtain the tenant ID
     */
    protected TenantDataOperation(Command command) {
        this(command.getContext().getTenantId(),
             command.getContext().getCommandId());
    }

    @VisibleForTesting
    TenantId tenantId() {
        return tenantId;
    }

    /**
     * Obtains ID of the currently handled command.
     *
     * @throws IllegalStateException of the method is called from non-command handling context
     */
    public CommandId commandId() {
        if (commandId == null) {
            throw new IllegalStateException("Unable to get CommandId from non-command handling excution context.");
        }
        return commandId;
    }

    /**
     * Executes the operation.
     *
     * <p>The execution goes through the following steps:
     * <ol>
     *     <li>The current tenant ID is obtained and remembered.
     *     <li>The tenant ID passed to the constructor is set as current.
     *     <li>The {@link #run()} method is called.
     *     <li>The previous tenant ID is set as current.
     * </ol>
     */
    public void execute() {
        final Optional<TenantId> remembered = CurrentTenant.get();
        try {
            CurrentTenant.set(tenantId());
            run();
        } finally {
            if (remembered.isPresent()) {
                CurrentTenant.set(remembered.get());
            } else {
                CurrentTenant.clear();
            }
        }
    }
}

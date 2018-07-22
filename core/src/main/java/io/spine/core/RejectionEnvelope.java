/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.core;

import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wraps the business rejection into a transferable parcel which provides a convenient access to
 * its properties.
 *
 * @author Alex Tymchenko
 */
public class RejectionEnvelope
        extends EnrichableMessageEnvelope<RejectionId, Rejection, RejectionContext> {

    /** The rejection message. */
    private final Message rejectionMessage;

    /** The class of the rejection. */
    private final RejectionClass rejectionClass;

    /** The message of a {@link Command}, processing of which triggered the rejection. */
    private final Message commandMessage;

    /** The context of a {@link Command}, which processing triggered the rejection. */
    private final CommandContext commandContext;

    private RejectionEnvelope(Rejection rejection) {
        super(rejection);
        this.rejectionMessage = Rejections.getMessage(rejection);
        this.rejectionClass = RejectionClass.of(rejectionMessage);
        RejectionContext context = rejection.getContext();
        Command command = context.getCommand();
        this.commandMessage = Commands.getMessage(command);
        this.commandContext = command.getContext();
    }

    /**
     * Creates instance for the passed rejection.
     */
    public static RejectionEnvelope of(Rejection rejection) {
        checkNotNull(rejection);
        return new RejectionEnvelope(rejection);
    }

    /**
     * Obtains the ID of the rejection.
     */
    @Override
    public RejectionId getId() {
        return getOuterObject().getId();
    }

    @Override
    public TenantId getTenantId() {
        return getActorContext().getTenantId();
    }

    @Override
    public Message getMessage() {
        return rejectionMessage;
    }

    @Override
    public RejectionClass getMessageClass() {
        return rejectionClass;
    }

    public RejectionContext getRejectionContext() {
        return getOuterObject().getContext();
    }

    @Override
    public ActorContext getActorContext() {
        return getCommandContext().getActorContext();
    }

    @Override
    public RejectionContext getMessageContext() {
        return getOuterObject().getContext();
    }

    /**
     * Sets the origin fields of the event context being built using the data of the enclosed
     * rejection.
     *
     * <p>In particular: 
     * <ul>
     *     <li>the ID of the command inside of the rejection context is set as the root command 
     *     identifier;</li>
     *     <li>the rejection context is set as the origin.</li>
     * </ul>
     *
     * @param builder event context builder into which the origin related fields are set
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    @Override
    public void setOriginFields(EventContext.Builder builder) {
        RejectionContext context = getOuterObject().getContext();
        builder.setRejectionContext(context);
        builder.setRootCommandId(context.getCommand()
                                        .getId());
    }

    public Message getCommandMessage() {
        return commandMessage;
    }

    public CommandContext getCommandContext() {
        return commandContext;
    }

    @Override
    public Enrichment getEnrichment() {
        return getMessageContext().getEnrichment();
    }

    @Override
    protected RejectionEnvelope enrich(Enrichment enrichment) {
        Rejection.Builder enrichedCopy =
                getOuterObject().toBuilder()
                                .setContext(getMessageContext().toBuilder()
                                                               .setEnrichment(enrichment));
        return of(enrichedCopy.build());
    }
}

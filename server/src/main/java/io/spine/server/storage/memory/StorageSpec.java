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

package io.spine.server.storage.memory;

import com.google.common.base.MoreObjects;
import io.spine.core.BoundedContextName;
import io.spine.type.TypeUrl;

import java.io.Serializable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Preconditions2.checkNotDefaultArg;

/**
 * Attributes for accessing in-memory storage over in-process gRPC.
 *
 * @param <I> the type of entity identifiers used by the storage
 */
public final class StorageSpec<I> implements Serializable {

    private static final long serialVersionUID = 0L;
    
    private static final String FLD_BOUNDED_CONTEXT_NAME = "context";
    private static final String FLD_ENTITY_STATE_URL = "entityStateUrl";
    private static final String FLD_ID_CLASS = "idClass";

    private final BoundedContextName context;
    private final TypeUrl entityStateUrl;
    private final Class<I> idClass;

    public static <I>
    StorageSpec<I> of(BoundedContextName context, TypeUrl entityStateUrl, Class<I> idClass) {
        checkNotDefaultArg(context);
        checkNotNull(entityStateUrl);
        checkNotNull(idClass);
        return new StorageSpec<>(context, entityStateUrl, idClass);
    }

    private StorageSpec(BoundedContextName context, TypeUrl entityStateUrl, Class<I> idClass) {
        this.context = context;
        this.entityStateUrl = entityStateUrl;
        this.idClass = idClass;
    }

    /**
     * Obtains the name of the context.
     * @deprecated please use {@link #context()}
     */
    @Deprecated
    public BoundedContextName getBoundedContextName() {
        return context();
    }

    /**
     * Obtains the name of the context served by the storage.
     */
    public BoundedContextName context() {
        return context;
    }

    /**
     * Obtains the URL of the entity state type.
     * @deprecated please use {@link #entityStateUrl()}
     */
    @Deprecated
    public TypeUrl getEntityStateUrl() {
        return entityStateUrl();
    }

    /**
     * Obtains the URL of the entity state type.
     */
    public TypeUrl entityStateUrl() {
        return entityStateUrl;
    }

    /**
     * Obtains the class of identifiers used by the storage.
     * @deprecated please use {@link #idClass()}
     */
    @Deprecated
    public Class<I> getIdClass() {
        return idClass();
    }

    /**
     * Obtains the class of identifiers used by the storage.
     */
    public Class<I> idClass() {
        return idClass;
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, entityStateUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        StorageSpec other = (StorageSpec) obj;
        return Objects.equals(this.context, other.context)
                && Objects.equals(this.entityStateUrl, other.entityStateUrl);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add(FLD_BOUNDED_CONTEXT_NAME, context)
                          .add(FLD_ENTITY_STATE_URL, entityStateUrl.value())
                          .add(FLD_ID_CLASS, idClass.getName())
                          .toString();
    }
}

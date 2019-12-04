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

package io.spine.client;

import com.google.protobuf.Message;
import io.spine.annotation.GeneratedMixin;
import io.spine.client.CompositeFilter.CompositeOperator;

import java.util.List;

/**
 * Augments {@link CompositeFilter} with useful methods.
 */
@GeneratedMixin
interface CompositeFilterMixin extends CompositeFilterOrBuilder, CompositeMessageFilter<Message> {

    @Override
    default List<MessageFilter<Message>> filters() {
        List<Filter> filterList = getFilterList();
        @SuppressWarnings("unchecked") /* The cast below is safe (see
         https://docs.oracle.com/javase/tutorial/java/generics/subtyping.html for details).
         `Filter` does implement `MessageFilter<Message>` via `FilterMixin` */
        List<MessageFilter<Message>> list =
                (List<MessageFilter<Message>>) (List<?>) filterList;
        return list;
    }

    @Override
    default CompositeOperator operator() {
        return getOperator();
    }
}
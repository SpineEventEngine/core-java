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

package io.spine.server.trace.stackdriver;

import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.ClientContext;
import com.google.cloud.trace.v2.stub.GrpcTraceServiceStub;
import com.google.devtools.cloudtrace.v2.AttributeValue;
import com.google.devtools.cloudtrace.v2.BatchWriteSpansRequest;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.TruncatableString;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.core.BoundedContextName;
import io.spine.core.MessageId;
import io.spine.core.Signal;
import io.spine.core.SignalId;
import io.spine.server.trace.AbstractTracer;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import static com.google.api.client.util.Lists.newArrayList;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.synchronizedList;

final class StackdriverTracer extends AbstractTracer {

    private final @Nullable BoundedContextName context;
    private final List<Span> spans;
    private final ApiCallContext callContext;
    private final String projectId;

    StackdriverTracer(Signal<?, ?, ?> signal,
                      ApiCallContext callContext,
                      String gcpProjectId,
                      @Nullable BoundedContextName context) {
        super(signal);
        this.projectId = gcpProjectId;
        this.context = context;
        this.callContext = checkNotNull(callContext);
        this.spans = synchronizedList(newArrayList());
    }

    @Override
    public void processedBy(MessageId receiver) {
        Timestamp whenStarted = signal().time();
        Timestamp whenFinished = Time.currentTime();
        Span span = newSpan(receiver.getTypeUrl(), whenStarted, whenFinished);
        spans.add(span);
    }

    private Span newSpan(String name, Timestamp whenStarted, Timestamp whenFinished) {
        String spanId = spanId(signal().id());
        Span.Builder span = Span
                .newBuilder()
                .setName(spanName())
                .setSpanId(spanId)
                .setDisplayName(truncatable(name))
                .setStartTime(whenStarted)
                .setEndTime(whenFinished);
        signal().parent()
                .map(parent -> spanId(parent.asSignalId()))
                .ifPresent(span::setParentSpanId);
        if (context != null) {
            AttributeValue attributeValue = AttributeValue
                    .newBuilder()
                    .setStringValue(truncatable(context.getValue()))
                    .build();
            span.getAttributesBuilder()
                .putAttributeMap(Attribute.context.qualifiedName(), attributeValue);
        }
        return span.build();
    }

    @Override
    public void close() throws Exception {
        GrpcTraceServiceStub stub = GrpcTraceServiceStub.create(ClientContext.newBuilder()
                                                                             .build());
        BatchWriteSpansRequest request = BatchWriteSpansRequest
                .newBuilder()
                .setName(projectName())
                .addAllSpans(spans)
                .build();
        stub.batchWriteSpansCallable()
            .call(request, callContext);
        stub.close();
    }

    private static TruncatableString truncatable(String initial) {
        return TruncatableString
                .newBuilder()
                .setValue(initial)
                .build();
    }

    /**
     * @see <a href="https://cloud.google.com/trace/docs/reference/v2/rpc/google.devtools.cloudtrace.v2#google.devtools.cloudtrace.v2.BatchWriteSpansRequest">API doc</a>
     */
    private String spanName() {
        String traceId = signal().rootMessage()
                                 .asSignalId()
                                 .value();
        String spanId = spanId(signal().id());
        return format("projects/%s/traces/%s/spans/%s", projectId, traceId, spanId);
    }

    /**
     * @see <a href="https://cloud.google.com/trace/docs/reference/v2/rpc/google.devtools.cloudtrace.v2#google.devtools.cloudtrace.v2.BatchWriteSpansRequest">API doc</a>
     */
    private String projectName() {
        return format("projects/%s", projectId);
    }

    private static String spanId(SignalId signalId) {
        return signalId.value();
    }

    private enum Attribute {

        context;

        private static final String ATTRIBUTE_PREFIX = "spine.io";

        private String qualifiedName() {
            return ATTRIBUTE_PREFIX + "/" + name();
        }
    }
}

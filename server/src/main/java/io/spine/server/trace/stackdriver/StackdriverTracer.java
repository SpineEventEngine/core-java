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
import io.spine.type.TypeName;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.UUID;

import static com.google.api.client.util.Lists.newArrayList;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Long.toHexString;
import static java.lang.String.format;
import static java.util.Collections.synchronizedList;

final class StackdriverTracer extends AbstractTracer {

    private final @Nullable BoundedContextName context;
    private final List<Span> spans;
    private final GrpcTraceServiceStub service;
    private final String projectId;

    StackdriverTracer(Signal<?, ?, ?> signal,
                      GrpcTraceServiceStub service,
                      String gcpProjectId,
                      @Nullable BoundedContextName context) {
        super(signal);
        this.service = service;
        this.projectId = gcpProjectId;
        this.context = checkNotNull(context);
        this.spans = synchronizedList(newArrayList());
    }

    @Override
    public void processedBy(MessageId receiver) {
        Timestamp whenStarted = signal().time();
        Timestamp whenFinished = Time.currentTime();
        Span span = newSpan(displayName(receiver), whenStarted, whenFinished);
        spans.add(span);
    }

    private String displayName(MessageId receiver) {
        TypeName signalType = signal().typeUrl()
                                      .toTypeName();
        TypeName receiverType = TypeUrl.parse(receiver.getTypeUrl())
                                       .toTypeName();
        return format("%s processes %s", receiverType, signalType);
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
    public void close() {
        BatchWriteSpansRequest request = BatchWriteSpansRequest
                .newBuilder()
                .setName(projectName())
                .addAllSpans(spans)
                .build();
        service.batchWriteSpansCallable()
            .call(request);
        service.close();
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
        String traceId = traceId(signal().rootMessage().asSignalId());
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
        return hexOfLength(signalId, 16);
    }

    private static String traceId(SignalId rootMessage) {
        return hexOfLength(rootMessage, 32);
    }

    private static String hexOfLength(SignalId id, int resultLength) {
        long mostSignificantBits = UUID.fromString(id.value())
                                       .getMostSignificantBits();
        long leastSignificantBits = UUID.fromString(id.value())
                                        .getLeastSignificantBits();
        String result = toHexString(mostSignificantBits) + toHexString(leastSignificantBits);
        return shorten(result, resultLength);
    }

    private static String shorten(String value, int length) {
        return value.length() > length
               ? value.substring(0, length)
               : value;
    }

    private enum Attribute {

        context;

        private static final String ATTRIBUTE_PREFIX = "spine.io";

        private String qualifiedName() {
            return ATTRIBUTE_PREFIX + '/' + name();
        }
    }
}

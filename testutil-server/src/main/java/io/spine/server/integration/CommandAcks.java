/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.integration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.Status;
import io.spine.grpc.MemoizingObserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
class CommandAcks {

    private static final Rejection EMPTY_REJECTION = Rejection.getDefaultInstance();
    private static final Error EMPTY_ERROR = Error.getDefaultInstance();

    private final List<Ack> acks = newArrayList();
    private final List<Error> errors = newArrayList();
    private final List<Rejection> rejections = newArrayList();
    private final Map<RejectionClass, Integer> rejectionTypes;

    CommandAcks(MemoizingObserver<Ack> observer) {
        List<Ack> responses = observer.responses();
        for (Ack response : responses) {
            acks.add(response);

            Status status = response.getStatus();

            Error error = status.getError();
            if (!error.equals(EMPTY_ERROR)) {
                errors.add(error);
            }

            Rejection rejection = status.getRejection();
            if (!rejection.equals(EMPTY_REJECTION)) {
                rejections.add(rejection);
            }
        }
        rejectionTypes = countRejectionTypes(rejections);
    }

    /**
     * Counts the number of times the domain event types are included in the provided list.
     *
     * @param rejections a list of {@link Event}
     * @return a mapping of Rejection classes to their count
     */
    private static Map<RejectionClass, Integer> countRejectionTypes(List<Rejection> rejections) {
        Map<RejectionClass, Integer> countForType = new HashMap<>();
        for (Rejection rejection : rejections) {
            RejectionClass type = RejectionClass.of(rejection);
            int currentCount = countForType.getOrDefault(type, 0);
            countForType.put(type, currentCount + 1);
        }
        return ImmutableMap.copyOf(countForType);
    }

    public int count() {
        return acks.size();
    }

    /*
     * Errors
     ******************************************************************************/

    boolean containNoErrors() {
        return errors.isEmpty();
    }

    boolean containErrors() {
        return !errors.isEmpty();
    }

    boolean containError(Error error) {
        checkNotNull(error);
        return errors.contains(error);
    }

    boolean containError(ErrorQualifier qualifier) {
        return errors.stream()
                     .anyMatch(qualifier);
    }

    /*
     * Rejections
     ******************************************************************************/

    boolean containNoRejections() {
        return rejections.isEmpty();
    }

    boolean containRejections() {
        return rejections.isEmpty();
    }

    boolean containRejections(RejectionClass type) {
        return rejectionTypes.containsKey(type);
    }

    boolean containRejection(Message domainRejection) {
        return rejections.stream()
                         .anyMatch(rejection -> {
                             Message message = unpack(rejection.getMessage());
                             return domainRejection.equals(message);
                         });
    }

    boolean containRejections(Message rejection1, Message rejection2, Message... otherRejections) {
        if (!containRejection(rejection1)) {
            return false;
        }
        if (!containRejection(rejection2)) {
            return false;
        }
        for (Message rejection : otherRejections) {
            if (!containRejection(rejection)) {
                return false;
            }
        }
        return true;
    }
}

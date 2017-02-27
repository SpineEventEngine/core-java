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

package org.spine3.server.reflect;

import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.server.aggregate.Apply;
import org.spine3.test.aggregate.event.ProjectCreated;
import org.spine3.testdata.Sample;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EventApplierMethodShould {

    private final HandlerMethod.Factory<EventApplierMethod> factory = EventApplierMethod.factory();

    @Test
    public void invoke_applier_method() throws InvocationTargetException {
        final ValidApplier applierObject = new ValidApplier();
        final EventApplierMethod applier = EventApplierMethod.from(applierObject.getMethod());
        final ProjectCreated event = Sample.messageOfType(ProjectCreated.class);

        applier.invoke(applierObject, event);

        assertEquals(event, applierObject.eventApplied);
    }

    @Test
    public void return_factory_instance() {
        assertNotNull(factory);
    }

    @Test
    public void return_method_class() {
        assertEquals(EventApplierMethod.class, factory.getMethodClass());
    }

    @Test
    public void create_method() {
        final Method method = new ValidApplier().getMethod();

        final EventApplierMethod actual = factory.create(method);

        assertEquals(EventApplierMethod.from(method), actual);
    }

    @Test
    public void return_method_predicate() {
        assertEquals(EventApplierMethod.predicate(), factory.getPredicate());
    }

    @Test
    public void check_method_access_modifier() {
        final Method method = new ValidApplierButNotPrivate().getMethod();

        factory.checkAccessModifier(method);
    }

    @Test
    public void consider_applier_with_one_msg_param_valid() {
        final Method applier = new ValidApplier().getMethod();

        assertIsEventApplier(applier);
    }

    @Test
    public void consider_not_private_applier_valid() {
        final Method method = new ValidApplierButNotPrivate().getMethod();

        assertIsEventApplier(method);
    }

    @Test
    public void consider_not_annotated_applier_invalid() {
        final Method applier = new InvalidApplierNoAnnotation().getMethod();

        assertIsNotEventApplier(applier);
    }

    @Test
    public void consider_applier_without_params_invalid() {
        final Method applier = new InvalidApplierNoParams().getMethod();

        assertIsNotEventApplier(applier);
    }

    @Test
    public void consider_applier_with_too_many_params_invalid() {
        final Method applier = new InvalidApplierTooManyParams().getMethod();

        assertIsNotEventApplier(applier);
    }

    @Test
    public void consider_applier_with_one_invalid_param_invalid() {
        final Method applier = new InvalidApplierOneNotMsgParam().getMethod();

        assertIsNotEventApplier(applier);
    }

    @Test
    public void consider_not_void_applier_invalid() {
        final Method applier = new InvalidApplierNotVoid().getMethod();

        assertIsNotEventApplier(applier);
    }

    private static void assertIsEventApplier(Method applier) {
        assertTrue(EventApplierMethod.predicate().apply(applier));
    }

    private static void assertIsNotEventApplier(Method applier) {
        assertFalse(EventApplierMethod.predicate().apply(applier));
    }

    /*
     * Valid appliers
     ****************/

    private static class ValidApplier extends TestEventApplier {

        private ProjectCreated eventApplied;

        @Apply
        private void apply(ProjectCreated event) {
            this.eventApplied = event;
        }
    }

    private static class ValidApplierButNotPrivate extends TestEventApplier {
        @Apply
        public void apply(ProjectCreated event) {
        }
    }

    /*
     * Invalid appliers
     *******************/

    private static class InvalidApplierNoAnnotation extends TestEventApplier {
        @SuppressWarnings("unused")
        public void apply(ProjectCreated event) {
        }
    }

    private static class InvalidApplierNoParams extends TestEventApplier {
        @Apply
        public void apply() {
        }
    }

    private static class InvalidApplierTooManyParams extends TestEventApplier {
        @Apply
        public void apply(ProjectCreated event, Object redundant) {
        }
    }

    private static class InvalidApplierOneNotMsgParam extends TestEventApplier {
        @Apply
        public void apply(Exception invalid) {
        }
    }

    private static class InvalidApplierNotVoid extends TestEventApplier {
        @Apply
        public Object apply(ProjectCreated event) {
            return event;
        }
    }

    private abstract static class TestEventApplier extends Aggregate<Long,
                                                                     StringValue,
                                                                     StringValue.Builder> {

        protected TestEventApplier() {
            super(0L);
        }

        private static final String APPLIER_METHOD_NAME = "apply";

        public Method getMethod() {
            final Method[] methods = getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(APPLIER_METHOD_NAME)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new RuntimeException("No applier method found: " + APPLIER_METHOD_NAME);
        }
    }
}
